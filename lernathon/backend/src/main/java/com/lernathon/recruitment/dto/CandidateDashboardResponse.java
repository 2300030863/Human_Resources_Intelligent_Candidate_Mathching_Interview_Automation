package com.lernathon.recruitment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidateDashboardResponse {

    private List<ApplicationSummary> recentApplications;
    private List<ExamResultSummary> examResults;
    private List<InterviewSummary> upcomingInterviews;

    @Data
    @Builder
    public static class ApplicationSummary {
        private Long applicationId;
        private String jobTitle;
        private String company;
        private String status;
        private LocalDateTime appliedAt;
        private Double matchScore;
    }

    @Data
    @Builder
    public static class ExamResultSummary {
        private Long applicationId;
        private String jobTitle;
        private Integer examScore;
        private String result; // PASSED / FAILED
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    public static class InterviewSummary {
        private Long applicationId;
        private String jobTitle;
        private LocalDateTime interviewDate;
        private String status;
        private String interviewResult;
    }
}
