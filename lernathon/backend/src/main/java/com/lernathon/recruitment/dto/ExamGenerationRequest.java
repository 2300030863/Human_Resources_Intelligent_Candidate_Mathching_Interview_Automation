package com.lernathon.recruitment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamGenerationRequest {
    @NotNull(message = "Candidate ID is required")
    private Long candidateId;
    
    @NotNull(message = "Job ID is required")
    private Long jobId;
    
    private Long applicationId;
    
    @NotNull(message = "Resume match score is required")
    @Min(value = 0, message = "Resume match score must be at least 0")
    private Double resumeMatchScore;
    
    // Question source: "AI" (default) or "CODEFORCES"
    @Builder.Default
    private String questionSource = "AI";
}
