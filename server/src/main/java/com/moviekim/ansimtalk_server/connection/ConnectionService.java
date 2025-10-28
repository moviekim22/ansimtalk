package com.moviekim.ansimtalk_server.connection;

import com.moviekim.ansimtalk_server.connection.dto.ConnectionResponseDto;
import com.moviekim.ansimtalk_server.user.Role;
import com.moviekim.ansimtalk_server.user.User;
import com.moviekim.ansimtalk_server.user.UserService; // <-- UserRepository 대신 UserService를 import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    public List<ConnectionResponseDto> getConnectedElderlyList(Long guardianId) {
        // 1. Repository를 통해 특정 보호자와 연결된 모든 Connection 목록을 DB에서 가져온다.
        List<Connection> connections = connectionRepository.findAllByGuardianId(guardianId);

        // 2. 가져온 Connection 목록을 순회하면서,
        //    각 Connection에 들어있는 어르신(User) 정보를 DTO로 변환한다.
        return connections.stream()
                .map(connection -> new ConnectionResponseDto(connection.getElderly()))
                .collect(Collectors.toList());
    }
}