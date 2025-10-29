package com.moviekim.ansimtalk_server.event;

import com.moviekim.ansimtalk_server.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class EventLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 이벤트가 발생한 사용자

    @Enumerated(EnumType.STRING)
    private EventType eventType; // 이벤트 종류

    private LocalDateTime eventTime; // 발생 시간
    private Double latitude; // 위도
    private Double longitude; // 경도

    public EventLog(User user, EventType eventType, Double latitude, Double longitude) {
        this.user = user;
        this.eventType = eventType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.eventTime = LocalDateTime.now();
    }
}