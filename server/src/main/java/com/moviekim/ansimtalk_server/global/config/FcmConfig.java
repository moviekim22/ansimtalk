package com.moviekim.ansimtalk_server.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class FcmConfig {

    @PostConstruct // 서버가 시작될 때 이 메소드를 자동으로 딱 한 번 실행함.
    public void initialize() {
        try {
            // 1단계에서 추가한 비공개 키 파일을 읽어옵니다.
            ClassPathResource resource = new ClassPathResource("fcm-private-key.json");
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Firebase 앱이 아직 초기화되지 않았다면, 지금 초기화합니다.
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase가 성공적으로 초기화되었습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        // FirebaseApp이 초기화된 후에 getInstance()를 호출해야 합니다.
        // initialize() 메소드가 @PostConstruct로 먼저 실행되므로 안전합니다.
        return FirebaseMessaging.getInstance();
    }
}