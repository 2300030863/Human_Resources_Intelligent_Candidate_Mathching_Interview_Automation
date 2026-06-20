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
public class QuestionDTO {
    private Long id;
    private String skill;
    private String level;
    private String type;
    private String question;
    private String codeSnippet;
    private List<String> options;
    private Integer points;
    private Integer timeLimit;
    private List<TestCaseDTO> testCases;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseDTO {
        private String input;
        private String expectedOutput;
    }
}
