package com.moviekim.ansimtalk_server.medication;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule schedule;

    @Column(nullable = false)
    private LocalDate date; // 복용한 날짜 (예: 2025-11-01)

    @Column(nullable = false)
    private boolean isTaken; // 복용 여부

    private LocalDateTime takenAt; // '복용' 버튼을 누른 시간

    @Builder
    public MedicationLog(MedicationSchedule schedule, LocalDate date, boolean isTaken, LocalDateTime takenAt) {
        this.schedule = schedule;
        this.date = date;
        this.isTaken = isTaken;
        this.takenAt = takenAt;
    }

    // (수정용) 복용 여부를 업데이트하는 메서드
    public void updateTaken(boolean isTaken) {
        this.isTaken = isTaken;
        this.takenAt = isTaken ? LocalDateTime.now() : null;
    }
}