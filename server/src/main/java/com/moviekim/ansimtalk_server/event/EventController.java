package com.moviekim.ansimtalk_server.event;

import com.moviekim.ansimtalk_server.TestController;
import com.moviekim.ansimtalk_server.event.dto.EventRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<String> receiveEvent(@RequestBody EventRequestDto requestDto) {
        eventService.processEvent(requestDto);
        return ResponseEntity.ok("긴급 신호가 성공적으로 접수되었습니다.");
    }

    @PostMapping("/emergency") // API 주소를 더 명확하게 변경
    public String testEmergencyCall(@RequestBody EventRequestDto requestDto) {
        System.out.println("send!");
        // EventService에 있는 긴급 알림 발송 로직을 호출합니다.
        eventService.sendEmergencyAlert(requestDto.getUserId());

        return "Urgent PUSH notification request completed";
    }
}