package com.lernathon.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompileResponse {
    private boolean success;
    private String status; // "SUCCESS" or "FAILED" for frontend compatibility
    private String message;
    private String error; // Compilation error details if any
}
