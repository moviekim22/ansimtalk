package com.moviekim.ansimtalk_server.user.dto;

import com.moviekim.ansimtalk_server.user.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSignUpRequestDto {
    private String loginId;
    private String password;
    private String name;
    private Role role; // 앱에서는 "GUARDIAN" 문자열, 서버에서는 Role Enum으로 자동 변환
}