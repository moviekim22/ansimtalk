package com.moviekim.ansimtalk_server.global.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map; // Map을 사용하기 위해 import 추가

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String token, String title, String body) {
        // 데이터가 없는 경우, 새로 만든 메서드에 null을 전달하여 호출합니다.
        sendNotificationWithData(token, title, body, null);
    }

    public void sendNotificationWithData(String token, String title, String body, Map<String, String> data) {
        // 1. 알림(Notification) 본문을 구성합니다. (앱이 꺼져있을 때 표시됨)
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // 2. 전체 메시지를 구성하는 Message.Builder를 생성합니다.
        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification);

        // 3. 데이터(data)가 존재하면 메시지에 추가합니다.
        // 이 데이터는 앱이 포그라운드/백그라운드 상태일 때 모두 onMessageReceived에서 수신됩니다.
        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        // 4. 최종 메시지를 빌드합니다.
        Message message = messageBuilder.build();

        try {
            // 5. Firebase에 메시지 전송을 요청합니다.
            String response = this.firebaseMessaging.send(message);
            System.out.println("PUSH 알림 발송 성공: " + response);
        } catch (Exception e) {
            // 에러 로그는 System.err로 출력하는 것이 더 좋습니다.
            System.err.println("PUSH 알림 발송 실패: " + e.getMessage());
            e.printStackTrace(); // 에러의 전체 내용을 확인하기 위해 추가
        }
    }
}