package com.moviekim.ansimtalk_server.event;

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

    @PostMapping("/emergency")
    public ResponseEntity<String> receiveEvent(@RequestBody EventRequestDto requestDto) {
        eventService.processEvent(requestDto); // 모든 로직은 processEvent가 담당
        return ResponseEntity.ok("긴급 신호가 성공적으로 접수되었습니다.");
    }
}