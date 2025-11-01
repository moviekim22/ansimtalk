package com.moviekim.ansimtalk_server.medication.dto;

import com.moviekim.ansimtalk_server.medication.MedicationLog;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class LogResponseDto {
    private Long logId;
    private String date;
    private boolean isTaken;
    private String takenAt; // "HH:mm" 또는 null
    private String medicationName;
    private String scheduleTime;

    public LogResponseDto(MedicationLog log) {
        this.logId = log.getId();
        this.date = log.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.isTaken = log.isTaken();
        this.takenAt = (log.getTakenAt() != null) ? log.getTakenAt().format(DateTimeFormatter.ofPattern("HH:mm")) : null;
        this.medicationName = log.getSchedule().getMedicationName();
        this.scheduleTime = log.getSchedule().getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}