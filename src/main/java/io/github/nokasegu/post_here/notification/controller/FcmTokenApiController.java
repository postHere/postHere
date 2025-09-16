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
 * FCM 토큰 업로드 API
 * - POST /api/push/token
 * - body: { token, platform, app }
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push")
public class FcmTokenApiController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public void upload(Principal principal, @RequestBody FcmTokenRequestDto req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token 필요");
        }
        fcmTokenService.upsert(principal, req);
    }
}
