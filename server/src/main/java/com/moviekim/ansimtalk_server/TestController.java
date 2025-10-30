package com.moviekim.ansimtalk_server;

import com.moviekim.ansimtalk_server.event.EventService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    // FcmService 대신 EventService를 주입받습니다.
    private final EventService eventService;

    @PostMapping("/api/test/emergency") // API 주소를 더 명확하게 변경
    public String testEmergencyCall(@RequestBody EmergencyTestRequestDto requestDto) {
        System.out.println("send!");
        // EventService에 있는 긴급 알림 발송 로직을 호출합니다.
        eventService.sendEmergencyAlert(requestDto.getElderlyId());

        return "Urgent PUSH notification request completed";
    }

    // 요청 DTO도 userId를 받도록 변경
    @Getter
    static class EmergencyTestRequestDto {
        private Long elderlyId;
    }
}