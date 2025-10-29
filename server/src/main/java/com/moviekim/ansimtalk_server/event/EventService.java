package com.moviekim.ansimtalk_server.event;

import com.moviekim.ansimtalk_server.connection.Connection;
import com.moviekim.ansimtalk_server.connection.ConnectionRepository;
import com.moviekim.ansimtalk_server.event.dto.EventRequestDto;
import com.moviekim.ansimtalk_server.global.fcm.FcmService;
import com.moviekim.ansimtalk_server.user.User;
import com.moviekim.ansimtalk_server.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventLogRepository eventLogRepository;
    private final UserService userService;
    private final ConnectionRepository connectionRepository;
    private final FcmService fcmService;

    public void processEvent(EventRequestDto requestDto) {
        // 1. 어떤 어르신에게서 이벤트가 발생했는지 찾는다.
        User elderly = userService.getUserById(requestDto.getUserId());

        // 2. 사건 일지(EventLog)를 DB에 기록한다.
        EventLog eventLog = new EventLog(elderly, requestDto.getEventType(), requestDto.getLatitude(), requestDto.getLongitude());
        eventLogRepository.save(eventLog);

        // 3. 이 어르신과 연결된 모든 보호자를 찾는다.
        List<Connection> connections = connectionRepository.findAllByElderlyId(elderly.getId());

        // 4. 모든 보호자에게 PUSH 알림을 보낸다.
        for (Connection connection : connections) {
            User guardian = connection.getGuardian();
            if (guardian.getFcmToken() != null) {
                String title = "🚨 긴급 상황 발생!";
                String body = elderly.getName() + "님에게 도움이 필요합니다!";
                fcmService.sendNotification(guardian.getFcmToken(), title, body);
            }
        }
    }
}