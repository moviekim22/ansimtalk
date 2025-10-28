package com.moviekim.ansimtalk_server.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User create(String loginId, String password, String name, Role role){
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword(password);
        user.setName(name);
        user.setRole(role);
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다. ID: " + id));
    }

    public User getUserByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디의 사용자를 찾을 수 없습니다."));
    }
}
