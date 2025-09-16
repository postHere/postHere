package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.dto.FcmTokenRequestDto;
import io.github.nokasegu.post_here.notification.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * POST /api/push/token
 * - 앱에서 발급받은 FCM registration token을 업로드
 * - Principal이 없으면 401
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push")
public class FcmTokenApiController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public void uploadToken(Principal principal, @RequestBody FcmTokenRequestDto body) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        fcmTokenService.saveOrUpdate(principal.getName(), body);
        // 200 OK (바디 없음)
    }
}
