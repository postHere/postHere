package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/push")
public class PushTokenController {

    private final UserInfoRepository userRepo;
    private final JdbcTemplate jdbc;

    @PostMapping("/token")
    public void saveToken(Principal principal, @RequestBody SaveTokenReq req) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        }
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is empty");
        }

        UserInfoEntity user = userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));

        // 엔티티 setter 유무에 관계없이 안정적으로 저장
        int updated = jdbc.update(
                "UPDATE user_info SET fcm_token = ?, updated_at = NOW() WHERE user_info_pk = ?",
                req.getToken(), user.getId()
        );

        log.info("[FCM] token saved userId={} updated={} head={}",
                user.getId(), updated,
                req.getToken().substring(0, Math.min(12, req.getToken().length())));

        // 204 No Content
    }

    @Data
    public static class SaveTokenReq {
        private String token;
        private String platform;
        private String app;
    }
}
