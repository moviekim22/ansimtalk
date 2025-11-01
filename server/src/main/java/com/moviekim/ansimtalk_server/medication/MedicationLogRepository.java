package com.moviekim.ansimtalk_server.medication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {
    // 특정 일정 + 특정 날짜의 기록이 있는지 찾기 (어르신 앱이 복용 체크 시 사용)
    Optional<MedicationLog> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

    // 특정 일정 + 날짜 범위의 모든 기록 찾기 (보호자 앱이 기록 조회 시 사용)
    List<MedicationLog> findAllByScheduleIdAndDateBetween(Long scheduleId, LocalDate startDate, LocalDate endDate);
}