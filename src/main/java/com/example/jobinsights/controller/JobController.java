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

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class JobController {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file, HttpSession session, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please upload a resume file.");
            return "resume-result";
        }

        try {
            // Extract text from PDF
            String resumeText;
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                resumeText = stripper.getText(document);
            }

            // Call OpenAI service with extracted text
            JsonNode jsonNode = openAIService.extractResumeInfo(resumeText);

            // Map JSON to ResumeInfo object
            ResumeInfo resumeInfo = objectMapper.treeToValue(jsonNode, ResumeInfo.class);

            // Save in session
            if (resumeInfo.getSkills() != null) {
                session.setAttribute("lastUploadedResumeSkills", resumeInfo.getSkills());
            }
            session.setAttribute("resumeInfo", resumeInfo);
            session.setAttribute("uploadedFileName", file.getOriginalFilename());

            // Save JSON for debugging
            String outputPath = System.getProperty("user.dir") + File.separator + "resume_info.json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonNode);

            // Add attributes for view
            model.addAttribute("resumeInfo", resumeInfo);
            model.addAttribute("uploadedFileName", file.getOriginalFilename());

        } catch (IOException e) {
            model.addAttribute("error", "Failed to process resume: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
        }

        // Go to resume-result page instead of index
        return "resume-result";
    }


    @PostMapping("/insights")
    public String getJobInsights(@RequestParam String role, HttpSession session, Model model) {
        try {
            JsonNode jsonNode = openAIService.getJobInsights(role);

            // Map JSON to JobInsights object
            JobInsights jobInsights = objectMapper.treeToValue(jsonNode, JobInsights.class);

            // Save JSON for debugging
            String outputPath = System.getProperty("user.dir") + File.separator + "job_insights.json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), jsonNode);

            model.addAttribute("jobInsights", jobInsights);

            // Job Score Matching
            List<String> lastUploadedResumeSkills = (List<String>) session.getAttribute("lastUploadedResumeSkills");
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

            // Add resume info + filename back to model
            model.addAttribute("resumeInfo", session.getAttribute("resumeInfo"));
            model.addAttribute("uploadedFileName", session.getAttribute("uploadedFileName"));

        } catch (IOException e) {
            model.addAttribute("error", "Failed to fetch job insights: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
        }

        return "result";
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        // Load from session if available
        Object resumeInfo = session.getAttribute("resumeInfo");
        Object uploadedFileName = session.getAttribute("uploadedFileName");

        model.addAttribute("resumeInfo", resumeInfo);
        model.addAttribute("uploadedFileName", uploadedFileName);

        return "index";
}
}
