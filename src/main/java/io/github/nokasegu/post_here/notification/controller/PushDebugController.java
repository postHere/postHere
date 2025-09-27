package io.github.nokasegu.post_here.notification.controller;

import com.google.firebase.FirebaseApp;
import io.github.nokasegu.post_here.notification.service.FcmSenderService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * 디버그 전용 REST 컨트롤러
 * - 브라우저만으로 FCM 환경/토큰/테스트 발송을 확인하기 위한 임시 엔드포인트
 * - 운영 배포 시에는 SecurityConfig로 관리자만 접근 허용하거나 비활성화하세요.
 */
@Slf4j
@RestController
@RequestMapping("/api/push/debug")
@RequiredArgsConstructor
public class PushDebugController {

    private final UserInfoRepository userRepo;
    private final FcmSenderService fcmSenderService;

    /**
     * 환경 점검: FirebaseApp 초기화/프로젝트 정보를 노출
     * GET /api/push/debug/env
     */
    @GetMapping("/env")
    public Map<String, Object> env() {
        Map<String, Object> out = new HashMap<>();
        out.put("firebaseApps", FirebaseApp.getApps().stream().map(FirebaseApp::getName).toList());
        out.put("defaultAppExists", !FirebaseApp.getApps().isEmpty());
        return out;
    }

    /**
     * 내 FCM 토큰 확인
     * GET /api/push/debug/my-token
     */
    @GetMapping("/my-token")
    public ResponseEntity<?> myToken(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인 필요"));
        }
        UserInfoEntity me = userRepo.findByEmail(principal.getName()).orElse(null);
        if (me == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "사용자 없음"));
        }
        String token = me.getFcmToken(); // A안: user_info.fcm_token 단일 컬럼 사용
        return ResponseEntity.ok(Map.of(
                "userId", me.getId(),
                "email", me.getEmail(),
                "tokenHead", token != null ? token.substring(0, Math.min(12, token.length())) : null,
                "token", token
        ));
    }

    /**
     * 단건 테스트 발송(나에게)
     * GET /api/push/debug/test-send
     * - user_info.fcm_token 으로 "테스트" 알림을 보냅니다.
     */
    @GetMapping("/test-send")
    public ResponseEntity<?> testSend(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인 필요"));
        }
        UserInfoEntity me = userRepo.findByEmail(principal.getName()).orElse(null);
        if (me == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "사용자 없음"));
        }
        String token = me.getFcmToken();
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "내 user_info.fcm_token 이 비어있음"));
        }
        try {
            String id = fcmSenderService.sendToToken(token, "테스트", "디버그 발송");
            return ResponseEntity.ok(Map.of("messageId", id));
        } catch (RuntimeException e) {
            // 내부적으로 FirebaseMessagingException을 감싸서 던지므로 메시지와 원인도 같이 반환
            Throwable cause = e.getCause();
            String msg = cause != null ? cause.toString() : e.toString();
            log.error("[DEBUG] test-send failed: {}", msg, e);
            return ResponseEntity.status(500).body(Map.of("error", "FCM send failed", "detail", msg));
        }
    }
}
