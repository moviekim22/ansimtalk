package com.moviekim.ansimtalk_server.medication.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScheduleRequestDto {
    private String medicationName;
    private String time; // "HH:mm" 형식의 문자열 (예: "09:00")
}