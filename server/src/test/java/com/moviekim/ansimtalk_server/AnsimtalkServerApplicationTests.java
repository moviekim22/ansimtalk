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
class AnsimtalkServerApplicationTests {

	@Autowired
	private UserService userService;

	@Test
	public void 사용자저장테스트(){
		// 보호자 계정 (ID: 1)
		User user1 = userService.create("guardian_test", "1234", "김보호", Role.GUARDIAN);
		// 어르신 계정 (ID: 2)
		User user2 = userService.create("elderly_test", "1234", "박어르신", Role.ELDERLY);
	}
}