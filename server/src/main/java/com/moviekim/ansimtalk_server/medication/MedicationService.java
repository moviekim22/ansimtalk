package com.moviekim.ansimtalk_server.medication;

import com.moviekim.ansimtalk_server.connection.ConnectionRepository;
import com.moviekim.ansimtalk_server.medication.dto.LogRequestDto;
import com.moviekim.ansimtalk_server.medication.dto.LogResponseDto;
import com.moviekim.ansimtalk_server.medication.dto.ScheduleRequestDto;
import com.moviekim.ansimtalk_server.medication.dto.ScheduleResponseDto;
import com.moviekim.ansimtalk_server.user.User;
import com.moviekim.ansimtalk_server.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicationService {

    private final UserService userService;
    private final ConnectionRepository connectionRepository; // 보호자-어르신 관계 확인용
    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationLogRepository logRepository;

    // 1. 어르신: 복약 일정 생성
    public void createSchedule(Long elderlyId, ScheduleRequestDto requestDto) {
        User elderly = userService.getUserById(elderlyId);
        LocalTime time = LocalTime.parse(requestDto.getTime(), DateTimeFormatter.ofPattern("HH:mm"));

        MedicationSchedule schedule = MedicationSchedule.builder()
                .elderly(elderly)
                .medicationName(requestDto.getMedicationName())
                .time(time)
                .isActive(true)
                .build();
        scheduleRepository.save(schedule);
    }

    // 2. 어르신: 나의 복약 일정 목록 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getSchedules(Long elderlyId) {
        return scheduleRepository.findAllByElderlyIdAndIsActiveTrue(elderlyId)
                .stream()
                .map(ScheduleResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 어르신: 약 복용 기록 (체크)
    public void recordMedicationLog(Long elderlyId, LogRequestDto requestDto) {
        MedicationSchedule schedule = scheduleRepository.findById(requestDto.getScheduleId())
                .orElseThrow(() -> new EntityNotFoundException("해당 약 일정을 찾을 수 없습니다."));

        // 보안 체크: 요청한 어르신 본인의 일정이 맞는지 확인
        if (!schedule.getElderly().getId().equals(elderlyId)) {
            throw new IllegalArgumentException("본인의 약 일정만 기록할 수 있습니다.");
        }

        LocalDate date = LocalDate.parse(requestDto.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        // 이미 해당 날짜에 기록이 있는지 확인
        Optional<MedicationLog> existingLog = logRepository.findByScheduleIdAndDate(schedule.getId(), date);

        if (existingLog.isPresent()) {
            // 기록이 있으면, isTaken 상태만 업데이트
            MedicationLog log = existingLog.get();
            log.updateTaken(requestDto.isTaken());
        } else {
            // 기록이 없으면, 새로 생성
            MedicationLog newLog = MedicationLog.builder()
                    .schedule(schedule)
                    .date(date)
                    .isTaken(requestDto.isTaken())
                    .takenAt(requestDto.isTaken() ? LocalDateTime.now() : null)
                    .build();
            logRepository.save(newLog);
        }
    }

    // 4. 보호자: 어르신의 복용 기록 목록 조회
    @Transactional(readOnly = true)
    public List<LogResponseDto> getLogsForGuardian(Long guardianId, Long elderlyId, int days) {
        // 보안 체크: 요청한 보호자와 어르신이 연결되어 있는지 확인
        // 1. ID로 User 객체를 각각 조회
        User guardian = userService.getUserById(guardianId);
        User elderly = userService.getUserById(elderlyId);

        // 2. User 객체로 Repository 메서드 호출
        if (!connectionRepository.existsByElderlyAndGuardian(guardian, elderly)) {
            throw new IllegalArgumentException("조회 권한이 없습니다. (연결된 관계 아님)");
        }

        // 1. 어르신의 모든 일정을 가져온다.
        List<MedicationSchedule> schedules = scheduleRepository.findAllByElderlyIdAndIsActiveTrue(elderlyId);

        // 2. 조회할 날짜 범위를 정한다 (오늘 ~ days일 전)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1); // 7일간이면 오늘 포함 6일 전

        // 3. 모든 일정에 대해 날짜 범위 내의 로그를 조회하고 DTO로 변환
        return schedules.stream()
                .flatMap(schedule ->
                        logRepository.findAllByScheduleIdAndDateBetween(schedule.getId(), startDate, endDate).stream()
                )
                .map(LogResponseDto::new)
                .collect(Collectors.toList());
    }
}