package com.lernathon.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotNull(message = "Exam attempt ID is required")
    private Long examAttemptId;
    
    @NotNull(message = "Question ID is required")
    private Long questionId;
    
    @NotBlank(message = "Answer is required")
    private String answer;
    
    @NotNull(message = "Time taken is required")
    private Integer timeTaken;
    
    @NotBlank(message = "Session token is required")
    private String sessionToken;
}
