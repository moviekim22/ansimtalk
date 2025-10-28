package com.moviekim.ansimtalk_server.connection.dto;

import com.moviekim.ansimtalk_server.user.User;
import lombok.Getter;

@Getter
public class ConnectionResponseDto {

    private final Long elderlyId;
    private final String elderlyName;

    public ConnectionResponseDto(User elderly) {
        this.elderlyId = elderly.getId();
        this.elderlyName = elderly.getName();
    }
}