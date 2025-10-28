package com.moviekim.ansimtalk_server;


import com.moviekim.ansimtalk_server.global.fcm.FcmService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final FcmService fcmService;

    @PostMapping("/api/test/fcm")
    public String testFcm(@RequestBody FcmRequestDto requestDto) {
        fcmService.sendNotification(
                requestDto.getToken(),
                "🚨 긴급 알림",
                "어르신께 긴급 상황이 발생했습니다!"
        );
        return "PUSH 알림 발송 요청이 완료되었습니다.";
    }

    @Getter
    static class FcmRequestDto {
        private String token;
    }
}