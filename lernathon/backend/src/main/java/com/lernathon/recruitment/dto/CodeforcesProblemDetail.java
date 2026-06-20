package com.lernathon.recruitment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeforcesProblemDetail {
    private String status;
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Problem> problems;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Problem {
        private String name;
        private Integer timeLimit;
        private Integer memoryLimit;
        private String input;
        private String output;
        private List<Test> tests;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Test {
        private String input;
        private String output;
    }
}
