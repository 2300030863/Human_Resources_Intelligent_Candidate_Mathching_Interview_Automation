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
public class ResumeUploadResponse {
    private String name;
    private String email;
    private String phone;
    private Integer experienceYears;
    private String education;
    private List<String> skills;
    private String message;
}
