package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {
    private Boolean isCorrect;
    private Integer pointsEarned;
    private String explanation;
    private Integer consecutiveCorrect;
    private Integer consecutiveWrong;
    private String difficultyChanged;
    private QuestionDTO nextQuestion;
}
