package com.example.jobinsights.controller;

import com.example.jobinsights.model.JobInsights;
import com.example.jobinsights.model.ResumeInfo;
import com.example.jobinsights.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/insights")
    public String getJobInsights(@RequestParam("role") String role, Model model) {
        try {
            JsonNode node = openAIService.getJobInsights(role);
            // If API returned raw text node with "raw", try to parse fields gracefully.
            JobInsights insights = objectMapper.treeToValue(node, JobInsights.class);

            model.addAttribute("role", role);
            model.addAttribute("insights", insights);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to fetch job insights: " + e.getMessage());
        }
        return "result";
    }

    @PostMapping("/uploadResume")
    public String uploadResume(@RequestParam("resume") MultipartFile resume, Model model) {
        if (resume == null || resume.isEmpty()) {
            model.addAttribute("error", "Please upload a PDF resume.");
            return "resume-result";
        }
        try (InputStream is = resume.getInputStream(); PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            JsonNode node = openAIService.extractResumeInfo(text);
            ResumeInfo resumeInfo = objectMapper.treeToValue(node, ResumeInfo.class);

            model.addAttribute("resumeInfo", resumeInfo);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to process resume: " + e.getMessage());
        }
        return "resume-result";
    }
}
