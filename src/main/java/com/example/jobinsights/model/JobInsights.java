package com.example.jobinsights.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobInsights {

    @JsonProperty("job_titles")
    private List<String> jobTitlesSnake;

    @JsonProperty("jobTitles")
    private List<String> jobTitlesCamel;

    private List<String> skills;
    private List<String> certifications;
    private List<String> education;

    // unified getter for Thymeleaf: insights.jobTitles
    public List<String> getJobTitles() {
        return jobTitlesCamel != null ? jobTitlesCamel : jobTitlesSnake;
    }

    public void setJobTitles(List<String> jobTitles) {
        this.jobTitlesCamel = jobTitles;
    }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public List<String> getEducation() { return education; }
    public void setEducation(List<String> education) { this.education = education; }
}
