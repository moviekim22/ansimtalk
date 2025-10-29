package com.moviekim.ansimtalk_server.user;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // 데이터베이스 테이블 이름 'users'로 지정
public class User {

    public User(String loginId, String password, String name, Role role){
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId; // 로그인 아이디

    @Column(nullable = false)
    private String password; // 비밀번호

    @Column(nullable = false, length = 30)
    private String name; // 이름

    @Enumerated(EnumType.STRING) // Enum 타입을 문자열("ELDERLY", "GUARDIAN")로 저장
    @Column(nullable = false)
    private Role role; // 역할 [어르신, 보호자]

    @Setter
    private String fcmToken; // PUSH 알림을 위한 FCM 토큰

}
