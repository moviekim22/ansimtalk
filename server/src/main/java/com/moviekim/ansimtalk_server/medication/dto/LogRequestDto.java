package com.moviekim.ansimtalk_server.medication.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogRequestDto {
    private Long scheduleId;
    private String date; // "yyyy-MM-dd" 형식의 문자열 (예: "2025-11-01")
    private boolean isTaken;
}