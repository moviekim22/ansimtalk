package com.moviekim.ansimtalk_server.user;

import com.moviekim.ansimtalk_server.user.dto.UserLoginRequestDto;
import com.moviekim.ansimtalk_server.user.dto.UserLoginResponseDto;
import com.moviekim.ansimtalk_server.user.dto.UserSignUpRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.moviekim.ansimtalk_server.user.dto.FcmTokenRequestDto;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserSignUpRequestDto requestDto) {
        userService.create(
                requestDto.getLoginId(),
                requestDto.getPassword(),
                requestDto.getName(),
                requestDto.getRole()
        );
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto requestDto) {
        // 아이디, 비밀번호를 따로 보내는 대신,
        // 'role'이 포함된 DTO 객체 자체를 서비스로 전달합니다.
        User user = userService.login(requestDto); // <-- 이 부분이 변경되었습니다!

        // User 엔티티를 응답용 DTO로 변환
        UserLoginResponseDto responseDto = new UserLoginResponseDto(user);

        // DTO 객체를 JSON으로 반환
        return ResponseEntity.ok(responseDto);
    }

    // FCM 토큰 업데이트 API (수정 없음)
    @PutMapping("/fcm-token")
    public ResponseEntity<String> updateFcmToken(@RequestBody FcmTokenRequestDto requestDto) {
        // DTO에서 userId와 fcmToken을 꺼내서 서비스에 전달합니다.
        userService.updateFcmToken(requestDto.getUserId(), requestDto.getFcmToken());
        return ResponseEntity.ok().build();
    }
}