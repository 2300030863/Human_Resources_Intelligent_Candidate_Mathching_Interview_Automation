package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamGenerationResponse {
    private Long examAttemptId;
    private String sessionToken;
    private Integer totalQuestions;
    private Integer timeLimit;
    private String difficulty;
    private String message;
    private List<QuestionDTO> questions;
}
