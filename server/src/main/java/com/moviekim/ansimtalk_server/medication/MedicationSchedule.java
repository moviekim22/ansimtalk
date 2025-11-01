package com.moviekim.ansimtalk_server.medication;

import com.moviekim.ansimtalk_server.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_id", nullable = false)
    private User elderly;

    @Column(nullable = false, length = 100)
    private String medicationName; // 약 이름

    @Column(nullable = false)
    private LocalTime time; // 복용 시간

    private boolean isActive = true;

    @Builder
    public MedicationSchedule(User elderly, String medicationName, LocalTime time, boolean isActive) {
        this.elderly = elderly;
        this.medicationName = medicationName;
        this.time = time;
        this.isActive = isActive;
    }
}