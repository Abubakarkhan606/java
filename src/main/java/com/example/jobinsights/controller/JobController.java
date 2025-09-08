package com.example.jobinsights.controller;

import com.example.jobinsights.model.JobInsights;
import com.example.jobinsights.model.ResumeInfo;
import com.example.jobinsights.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class JobController {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private ObjectMapper objectMapper;

    // Keep last uploaded resume skills in memory for score matching
    private List<String> lastUploadedResumeSkills;

    // ------------------- Resume Upload -------------------
    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please upload a resume file.");
            return "resume-result";
        }

        try {
            // ✅ Step 1: Extract text from PDF
            String resumeText;
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                resumeText = stripper.getText(document);
            }

            // ✅ Step 2: Call OpenAI service with extracted text
            JsonNode jsonNode = openAIService.extractResumeInfo(resumeText);

            // ✅ Step 3: Map JSON to ResumeInfo object
            ResumeInfo resumeInfo = objectMapper.treeToValue(jsonNode, ResumeInfo.class);

            // ✅ Step 4: Save last uploaded resume skills for scoring
            if (resumeInfo.getSkills() != null) {
                lastUploadedResumeSkills = resumeInfo.getSkills();
            }

            // (Optional) Save JSON for debugging
            String outputPath = System.getProperty("user.dir") + File.separator + "resume_info.json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonNode);

            model.addAttribute("resumeInfo", resumeInfo);

        } catch (IOException e) {
            model.addAttribute("error", "Failed to process resume: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
        }

        return "resume-result";
    }

    // ------------------- Job Insights -------------------
    @PostMapping("/insights")
    public String getJobInsights(@RequestParam String role, Model model) {
        try {
            JsonNode jsonNode = openAIService.getJobInsights(role);

            // ✅ Map JSON to JobInsights object
            JobInsights jobInsights = objectMapper.treeToValue(jsonNode, JobInsights.class);

            // (Optional) Save JSON for debugging
            String outputPath = System.getProperty("user.dir") + File.separator + "job_insights.json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonNode);

            model.addAttribute("jobInsights", jobInsights);

            // ✅ Job Score Matching
            if (lastUploadedResumeSkills != null && !lastUploadedResumeSkills.isEmpty()) {
                int matchCount = 0;
                for (String skill : jobInsights.getSkills()) {
                    if (lastUploadedResumeSkills.stream()
                            .anyMatch(resumeSkill -> resumeSkill.equalsIgnoreCase(skill))) {
                        matchCount++;
                    }
                }

                int totalSkills = jobInsights.getSkills().size();
                double percentage = totalSkills > 0 ? ((double) matchCount / totalSkills) * 100 : 0;

                model.addAttribute("jobScore", matchCount + "/" + totalSkills);
                model.addAttribute("jobScorePercentage", String.format("%.2f", percentage));
            } else {
                model.addAttribute("jobScore", "No resume uploaded");
                model.addAttribute("jobScorePercentage", null);
            }

        } catch (IOException e) {
            model.addAttribute("error", "Failed to fetch job insights: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
        }

        return "result";
    }
}
