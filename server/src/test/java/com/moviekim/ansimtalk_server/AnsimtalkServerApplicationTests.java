package com.moviekim.ansimtalk_server;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.moviekim.ansimtalk_server.global.fcm.FcmService; // FcmService import
import com.moviekim.ansimtalk_server.user.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional // (매우 중요!) 테스트가 끝나면 DB를 원래 상태로 롤백합니다.
class AnsimtalkServerApplicationTests {

	@Autowired
	private UserService userService;

	@Autowired // 검증을 위해 Repository도 직접 사용합니다.
	private UserRepository userRepository;
}