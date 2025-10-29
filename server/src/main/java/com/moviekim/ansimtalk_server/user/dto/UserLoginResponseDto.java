package com.moviekim.ansimtalk_server.user.dto;

import com.moviekim.ansimtalk_server.user.User;
import lombok.Getter;

@Getter
public class UserLoginResponseDto {
    private final Long id;
    private final String loginId;
    private final String name;

    // User 엔티티를 받아서 DTO로 변환하는 생성자
    public UserLoginResponseDto(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.name = user.getName();
    }
}