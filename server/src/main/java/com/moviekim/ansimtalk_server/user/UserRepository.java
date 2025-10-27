package com.moviekim.ansimtalk_server.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인 아이디로 사용자를 찾는 메소드
    Optional<User> findByLoginId(String loginId);
}