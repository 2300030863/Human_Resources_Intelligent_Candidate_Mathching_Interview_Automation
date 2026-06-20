package com.lernathon.recruitment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeforcesResponse {
    private String status;
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Problem> problems;
        private List<ProblemStatistics> problemStatistics;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Problem {
        private Integer contestId;
        private String index;
        private String name;
        private String type;
        private Integer points;
        private Integer rating;
        private List<String> tags;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProblemStatistics {
        private Integer contestId;
        private String index;
        private Integer solvedCount;
    }
}
