package com.lernathon.recruitment.dto;

import com.lernathon.recruitment.service.CodeExecutionService;
import lombok.Data;

import java.util.List;

@Data
public class TestCodeRequest {
    private String code;
    private List<CodeExecutionService.TestCase> testCases;
    private String language;
}
