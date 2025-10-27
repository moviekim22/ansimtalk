package com.moviekim.ansimtalk_server.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void create(String loginId, String password, String name, Role role){
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword(password);
        user.setName(name);
        user.setRole(role);
        userRepository.save(user);
    }

    public User getUser(String loginId) {
        return this.userRepository.findByLoginId(loginId)
                .orElseThrow();
    }
}
