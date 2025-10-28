package com.moviekim.ansimtalk_server.connection;

import com.moviekim.ansimtalk_server.connection.dto.ConnectionRequestDto;
import com.moviekim.ansimtalk_server.connection.dto.ConnectionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping
    public ResponseEntity<String> createConnection(@RequestBody ConnectionRequestDto requestDto) {
        // TODO: 실제로는 JWT 토큰 등에서 현재 로그인한 보호자의 ID를 가져와야 합니다.
        // 지금은 임시로 보호자 ID를 5번 사용자로 가정합니다.
        Long currentGuardianId = 5L;

        connectionService.createConnection(currentGuardianId, requestDto.getElderlyLoginId());

        return ResponseEntity.ok("성공적으로 연결되었습니다.");
    }

    @GetMapping
    public ResponseEntity<List<ConnectionResponseDto>> getMyConnections() {
        // TODO: 실제로는 JWT 토큰 등에서 현재 로그인한 보호자의 ID를 가져와야 합니다.
        Long currentGuardianId = 5L;

        List<ConnectionResponseDto> connectedList = connectionService.getConnectedElderlyList(currentGuardianId);

        return ResponseEntity.ok(connectedList);
    }
}
