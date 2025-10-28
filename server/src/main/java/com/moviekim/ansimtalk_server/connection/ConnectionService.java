package com.moviekim.ansimtalk_server.connection;

import com.moviekim.ansimtalk_server.user.Role;
import com.moviekim.ansimtalk_server.user.User;
import com.moviekim.ansimtalk_server.user.UserService; // <-- UserRepository 대신 UserService를 import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserService userService;

    public void createConnection(Long guardianId, String elderlyLoginId) {
        // 1. UserService를 통해 보호자 정보를 찾아온다.
        User guardian = userService.getUserById(guardianId);

        // 2. UserService를 통해 어르신 정보를 찾아온다.
        User elderly = userService.getUserByLoginId(elderlyLoginId);

        // 3. 역할 검증
        if (guardian.getRole() != Role.GUARDIAN) {
            throw new IllegalArgumentException("요청한 사용자가 보호자가 아닙니다.");
        }
        if (elderly.getRole() != Role.ELDERLY) {
            throw new IllegalArgumentException("연결하려는 대상이 어르신이 아닙니다.");
        }

        // 4. 이미 연결되어 있는지 확인
        if (connectionRepository.existsByElderlyAndGuardian(elderly, guardian)) {
            throw new IllegalArgumentException("이미 연결된 관계입니다.");
        }

        // 5. 새로운 연결 객체를 만들어 저장한다.
        Connection newConnection = new Connection(elderly, guardian);
        connectionRepository.save(newConnection);
    }
}