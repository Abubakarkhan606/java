package com.example.jobinsights.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

@Service
public class OpenAIService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;

    public OpenAIService() {
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(60))
                .build();

        // Try environment variable first; fallback to .env if present
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            try {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                key = dotenv.get("OPENAI_API_KEY");
            } catch (Exception ignored) { }
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("OpenAI API key not found. Set OPENAI_API_KEY env var or .env file.");
        }
        this.apiKey = key;
    }

    /** Request job insights. Returns a JsonNode (object with arrays). */
    public JsonNode getJobInsights(String role) throws IOException {
        String system = "You are a career assistant. Return ONLY a JSON object (no extra text) with keys: "
                + "\"job_titles\" (array of strings), \"certifications\" (array), \"skills\" (array), \"education\" (array or object).";
        String user = "Provide job insights for the role: " + role + ".";

        return callChatCompletion(system, user);
    }

    /** Extract structured info from resume text. Returns JsonNode. */
    public JsonNode extractResumeInfo(String resumeText) throws IOException {
        String system = "You are a resume parser. Return ONLY a JSON object (no extra text) with keys: "
                + "\"full_name\" (string), \"current_role\" (string), \"skills\" (array of strings), "
                + "\"education\" (object with optional fields), \"certifications\" (array).";
        String user = "Extract the individual's information from the resume text below. Respond with strict JSON only:\n\n" + resumeText;
        return callChatCompletion(system, user);
    }

    private JsonNode callChatCompletion(String systemMsg, String userMsg) throws IOException {
        // Build request body
        // Using gpt-3.5-turbo model by default
        ObjectMapper om = mapper;
        ObjectNode payload = om.createObjectNode();
        payload.put("model", "gpt-3.5-turbo");
        ArrayNode messages = om.createArrayNode();
        messages.add(om.createObjectNode().put("role", "system").put("content", systemMsg));
        messages.add(om.createObjectNode().put("role", "user").put("content", userMsg));
        payload.set("messages", messages);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 800);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(OPENAI_CHAT_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .post(body)
                .build();

        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                String txt = resp.body() != null ? resp.body().string() : "no body";
                throw new IOException("OpenAI API error: " + resp.code() + " - " + txt);
            }
            String respBody = Objects.requireNonNull(resp.body()).string();
            JsonNode root = om.readTree(respBody);

            // The chat response text is in choices[0].message.content
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode contentNode = choices.get(0).path("message").path("content");
                if (contentNode.isMissingNode()) {
                    // fallback: try 'text' style
                    contentNode = choices.get(0).path("text");
                }
                // content might be plain text or a JSON string (maybe fenced)
                String content = contentNode.asText().trim();
                String cleaned = stripCodeFences(content);
                // try parse as JSON
                try {
                    return om.readTree(cleaned);
                } catch (Exception e) {
                    // not valid JSON — wrap as { "raw": "<content>" }
                    ObjectNode fallback = om.createObjectNode();
                    fallback.put("raw", content);
                    return fallback;
                }
            } else {
                throw new IOException("No choices in OpenAI response");
            }
        }
    }

    private String stripCodeFences(String s) {
        if (s == null) return "";
        String t = s.trim();
        // remove leading ```json\n or ```\n
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) t = t.substring(firstNewline + 1);
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
        }
        return t.trim();
    }
}
