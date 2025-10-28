package com.moviekim.ansimtalk_server.global.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String token, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token) // 알림을 받을 대상의 FCM 토큰
                .setNotification(notification)
                .build();

        try {
            String response = this.firebaseMessaging.send(message);
            System.out.println("PUSH 알림 발송 성공: " + response);
        } catch (Exception e) {
            System.out.println("PUSH 알림 발송 실패: " + e.getMessage());
        }
    }
}