package com.lernathon.recruitment.dto;

import lombok.Data;

@Data
public class CompileRequest {
    private String code;
    private String language; // java, python, javascript
}
