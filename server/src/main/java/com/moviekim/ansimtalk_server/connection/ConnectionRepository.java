package com.moviekim.ansimtalk_server.connection;

import com.moviekim.ansimtalk_server.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    // 어르신과 보호자 정보로 이미 연결이 존재하는지 확인하는 메소드
    boolean existsByElderlyAndGuardian(User elderly, User guardian);

    // 보호자 ID로 모든 연결 정보를 리스트 형태로 찾아오는 기능
    List<Connection> findAllByGuardianId(Long guardianId);

    List<Connection> findAllByElderlyId(Long elderlyId);
}