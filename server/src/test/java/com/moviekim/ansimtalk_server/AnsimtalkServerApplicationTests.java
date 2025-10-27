package com.moviekim.ansimtalk_server;

import com.moviekim.ansimtalk_server.user.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnsimtalkServerApplicationTests {

	@Autowired
	private UserService userService;

	@Test
	void contextLoads() {
	}

	@Test
	void 회원가입테스트(){
		userService.create("로그인 아이디", "1234", "김영화", Role.GUARDIAN);
	}
}
