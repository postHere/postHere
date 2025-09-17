package io.github.nokasegu.post_here.notification.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class FcmTokenApiController {

    private static final Logger log = LoggerFactory.getLogger(FcmTokenApiController.class);
    private final JdbcTemplate jdbcTemplate;

    public FcmTokenApiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 기존: fcm_token 테이블 upsert → 변경: user_info.fcm_token 칼럼 업데이트
     */
    @PostMapping("/token")
    @Transactional
    public ResponseEntity<?> upload(@RequestBody UploadTokenRequest req, Authentication auth) {
        if (req == null || req.token == null || req.token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
        }
        final String email = auth.getName(); // 프로젝트 로그 상 email 기반 인증 사용
        int updated = jdbcTemplate.update(
                "UPDATE user_info SET fcm_token = ? WHERE email = ?",
                req.token, email
        );
        log.info("FCM token updated. email={}, updated={}", email, updated);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    /**
     * 디버그용: 내 토큰 확인 (선택)
     */
    @GetMapping("/token")
    public ResponseEntity<?> myToken(Authentication auth) {
        String email = auth.getName();
        String token = jdbcTemplate.query(
                "SELECT fcm_token FROM user_info WHERE email = ?",
                ps -> ps.setString(1, email),
                rs -> rs.next() ? rs.getString(1) : null
        );
        return ResponseEntity.ok(Map.of("email", email, "fcm_token", token));
    }

    public static class UploadTokenRequest {
        public String token;
        public String platform;   // optional
        public String appName;    // optional
    }
}
