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
public class ExamResultResponse {
    private Long examAttemptId;
    private String status;
    private Double finalScore;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private Integer cheatingScore;
    private Integer warningCount; // Number of cheat warnings/events
    private Boolean autoSubmitted;
    private String disqualificationReason;
    private Boolean qualifiedForInterview; // Whether candidate qualifies for interview
    private String message; // Result message for candidate
    private List<AnswerDetailDTO> answers;
}
