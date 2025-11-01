package com.moviekim.ansimtalk_server.user.dto;

import com.moviekim.ansimtalk_server.user.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLoginRequestDto {
    private String loginId;
    private String password;
    private Role role;
}