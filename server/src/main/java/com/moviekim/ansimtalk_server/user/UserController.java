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
        User user = userService.login(requestDto.getLoginId(), requestDto.getPassword());

        // User 엔티티를 응답용 DTO로 변환
        UserLoginResponseDto responseDto = new UserLoginResponseDto(user);

        // DTO 객체를 JSON으로 반환
        return ResponseEntity.ok(responseDto);
    }
}