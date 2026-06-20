package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunCodeResponse {
    private String status; // "SUCCESS", "FAILED", "TIMEOUT"
    private String output;
    private String error; // Runtime error details if any
    private String executionTime; // e.g., "342ms"
}
