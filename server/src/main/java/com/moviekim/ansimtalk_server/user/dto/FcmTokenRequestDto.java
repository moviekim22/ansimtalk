package com.moviekim.ansimtalk_server.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRequestDto {
    private Long userId;
    private String fcmToken;
}