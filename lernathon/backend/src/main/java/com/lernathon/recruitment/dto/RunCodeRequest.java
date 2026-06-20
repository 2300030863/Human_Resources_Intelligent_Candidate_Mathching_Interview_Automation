package com.lernathon.recruitment.dto;

import lombok.Data;

@Data
public class RunCodeRequest {
    private String code;
    private String input; // Custom input from candidate
    private String language; // java, python, javascript
}
