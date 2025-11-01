package com.moviekim.ansimtalk_server.medication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
    // 특정 어르신의 모든 활성화된 약 일정을 찾기
    List<MedicationSchedule> findAllByElderlyIdAndIsActiveTrue(Long elderlyId);
}