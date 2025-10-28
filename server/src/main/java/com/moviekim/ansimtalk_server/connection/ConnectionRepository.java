package com.moviekim.ansimtalk_server.connection;

import com.moviekim.ansimtalk_server.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    // 어르신과 보호자 정보로 이미 연결이 존재하는지 확인하는 메소드
    boolean existsByElderlyAndGuardian(User elderly, User guardian);
}