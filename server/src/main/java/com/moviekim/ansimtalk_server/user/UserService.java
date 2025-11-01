package com.moviekim.ansimtalk_server.user;

import com.moviekim.ansimtalk_server.user.dto.UserLoginRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 회원가입
    @Transactional
    public User create(String loginId, String password, String name, Role role) {
        // 아이디 중복 검사
        if (userRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = new User(loginId, password, name, role);
        return userRepository.save(user);
    }

    // 로그인
    public User login(UserLoginRequestDto requestDto) {
        // 1. 아이디로 사용자를 찾는다.
        User user = userRepository.findByLoginId(requestDto.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        // 2. 비밀번호가 일치하는지 확인한다.
        // TODO: 실제로는 암호화된 비밀번호를 비교해야 합니다.
        if (!user.getPassword().equals(requestDto.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 역할(Role)이 일치하는지 검사
        // DB에 저장된 역할과 앱에서 요청한 역할이 다르면 로그인 차단
        if (user.getRole() != requestDto.getRole()) {
            throw new IllegalArgumentException("로그인 권한이 없습니다. (역할 불일치)");
        }

        // 4. 모든 검사 통과
        return user;
    }

    // ID로 사용자 조회
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다. ID: " + id));
    }

    // 로그인 아이디로 사용자 조회
    public User getUserByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디의 사용자를 찾을 수 없습니다."));
    }

    // FCM 토큰 업데이트
    @Transactional
    public void updateFcmToken(Long id, String fcmToken) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다. ID: " + id));
        user.setFcmToken(fcmToken);
    }
}