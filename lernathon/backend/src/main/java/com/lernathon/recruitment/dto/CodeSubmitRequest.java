package com.lernathon.recruitment.dto;

import com.lernathon.recruitment.service.CodeExecutionService;
import lombok.Data;

import java.util.List;

@Data
public class CodeSubmitRequest {
    private String code;
    private List<CodeExecutionService.TestCase> testCases;
    private String language;
    private Long questionId;
    private Long examAttemptId;
    private String sessionToken;
    private Integer maxMarks;
}
