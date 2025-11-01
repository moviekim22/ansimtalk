package com.moviekim.ansimtalk_server.medication;

import com.moviekim.ansimtalk_server.medication.dto.LogRequestDto;
import com.moviekim.ansimtalk_server.medication.dto.LogResponseDto;
import com.moviekim.ansimtalk_server.medication.dto.ScheduleRequestDto;
import com.moviekim.ansimtalk_server.medication.dto.ScheduleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    // 1. 어르신: 복약 일정 생성
    @PostMapping("/schedules")
    public ResponseEntity<String> createSchedule(@RequestBody ScheduleRequestDto requestDto) {
        // TODO: JWT 인증 적용 후 @AuthenticationPrincipal 에서 어르신 ID 가져오기
        Long tempElderlyId = 1L; // 임시 어르신 ID (테스트용)
        medicationService.createSchedule(tempElderlyId, requestDto);
        return ResponseEntity.ok("약 일정이 등록되었습니다.");
    }

    // 2. 어르신: 나의 복약 일정 목록 조회
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponseDto>> getMySchedules() {
        // TODO: JWT 인증 적용 후 @AuthenticationPrincipal 에서 어르신 ID 가져오기
        Long tempElderlyId = 1L; // 임시 어르신 ID (테스트용)
        List<ScheduleResponseDto> schedules = medicationService.getSchedules(tempElderlyId);
        return ResponseEntity.ok(schedules);
    }

    // 3. 어르신: 약 복용 기록 (체크)
    @PostMapping("/logs")
    public ResponseEntity<String> recordMedicationLog(@RequestBody LogRequestDto requestDto) {
        // TODO: JWT 인증 적용 후 @AuthenticationPrincipal 에서 어르신 ID 가져오기
        Long tempElderlyId = 1L; // 임시 어르신 ID (테스트용)
        medicationService.recordMedicationLog(tempElderlyId, requestDto);
        return ResponseEntity.ok("복용 기록이 저장되었습니다.");
    }

    // 4. 보호자: 어르신의 복용 기록 목록 조회 (최근 7일)
    @GetMapping("/logs/guardian/{elderlyId}")
    public ResponseEntity<List<LogResponseDto>> getLogsForGuardian(
            @PathVariable Long elderlyId,
            @RequestParam(defaultValue = "7") int days) {

        // TODO: JWT 인증 적용 후 @AuthenticationPrincipal 에서 보호자 ID 가져오기
        Long tempGuardianId = 5L; // 임시 보호자 ID (테스트용, ConnectionController 참고)

        List<LogResponseDto> logs = medicationService.getLogsForGuardian(tempGuardianId, elderlyId, days);
        return ResponseEntity.ok(logs);
    }
}