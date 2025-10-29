package com.moviekim.ansimtalk_server.event.dto;

import com.moviekim.ansimtalk_server.event.EventType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventRequestDto {
    private Long userId;
    private EventType eventType;
    private Double latitude;
    private Double longitude;
}