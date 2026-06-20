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
public class AIResumeScreeningResponse {
    private Long candidateId;
    private String name;
    private String email;
    private String phone;
    private Integer experienceYears;
    private String education;
    private List<String> skills;
    private String status;
    private String message;
    private boolean isNewCandidate;
    private String currentCompany;
}
