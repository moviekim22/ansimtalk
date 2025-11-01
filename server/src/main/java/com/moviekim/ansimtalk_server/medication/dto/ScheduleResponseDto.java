package com.moviekim.ansimtalk_server.medication.dto;

import com.moviekim.ansimtalk_server.medication.MedicationSchedule;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class ScheduleResponseDto {
    private Long id;
    private String medicationName;
    private String time;
    private boolean isActive;

    public ScheduleResponseDto(MedicationSchedule schedule) {
        this.id = schedule.getId();
        this.medicationName = schedule.getMedicationName();
        this.time = schedule.getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.isActive = schedule.isActive();
    }
}