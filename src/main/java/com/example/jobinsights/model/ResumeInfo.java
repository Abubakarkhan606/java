package com.example.jobinsights.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeInfo {

    @JsonProperty("full_name")
    private String fullNameSnake;

    @JsonProperty("fullName")
    private String fullNameCamel;

    @JsonProperty("current_role")
    private String currentRoleSnake;

    @JsonProperty("currentRole")
    private String currentRoleCamel;

    private List<String> skills;

    private Education education;

    private List<String> certifications;

    public String getFullName() {
        return (fullNameCamel != null && !fullNameCamel.isBlank()) ? fullNameCamel : fullNameSnake;
    }
    public void setFullName(String name) { this.fullNameCamel = name; }

    public String getCurrentRole() {
        return (currentRoleCamel != null && !currentRoleCamel.isBlank()) ? currentRoleCamel : currentRoleSnake;
    }
    public void setCurrentRole(String r) { this.currentRoleCamel = r; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public Education getEducation() { return education; }
    public void setEducation(Education education) { this.education = education; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Education {
        private String degree;
        private String institution;
        private String location;
        private Duration duration;

        public String getDegree() { return degree; }
        public void setDegree(String degree) { this.degree = degree; }

        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public Duration getDuration() { return duration; }
        public void setDuration(Duration duration) { this.duration = duration; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Duration {
        private String start;
        private String end;

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
}
