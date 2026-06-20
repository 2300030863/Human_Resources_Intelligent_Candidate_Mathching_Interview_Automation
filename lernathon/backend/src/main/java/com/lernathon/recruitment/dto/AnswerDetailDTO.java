package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDetailDTO {
    private String question;
    private String candidateAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Integer pointsEarned;
}
