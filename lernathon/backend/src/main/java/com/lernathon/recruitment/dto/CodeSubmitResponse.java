package com.lernathon.recruitment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CodeSubmitResponse {
    private String status; // PASS, PARTIAL_PASS, FAIL, ERROR
    private int totalTestCases;
    private int passed;
    private double marksAwarded;
    private double maxMarks;
    private List<TestCaseDetail> details;
    private String message;

    @Data
    @Builder
    public static class TestCaseDetail {
        private int testCase;
        private String status; // PASSED, FAILED
        private String expected;
        private String received;
    }
}
