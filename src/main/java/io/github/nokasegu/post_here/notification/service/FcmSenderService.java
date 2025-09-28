// src/main/java/io/github/nokasegu/post_here/notification/service/FcmSenderService.java
package io.github.nokasegu.post_here.notification.service;

import com.google.firebase.messaging.*;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * FcmSenderService
 * <p>
 * [역할]
 * - FirebaseMessaging을 사용해 단건/멀티캐스트/폴백 발송 처리
 * <p>
 * [변경/추가]
 * - [추가 로그] 토큰 수집/개수/꼬리/발송 성패/예외를 INFO/WARN 로그로 더 명확히 기록
 * - [안정성] 빈 토큰 리스트 시 명시적으로 skip 로그 남김
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmSenderService {

    private final FirebaseMessaging firebaseMessaging;

    private static String tail(String token) {
        if (token == null) return "null";
        int n = token.length();
        return n <= 6 ? token : token.substring(n - 6);
    }

    // ===== Public API =====
    public String sendToToken(String token, String title, String body) {
        return sendToToken(token, title, body, Map.of());
    }

    public String sendToToken(String token, String title, String body, Map<String, String> data) {
        Objects.requireNonNull(token, "token must not be null");
        try {
            Message message = buildMessage(token, title, body, data);
            String messageId = firebaseMessaging.send(message);
            // [추가 로그]
            log.info("[FCM] Sent(one) id={}, tokenTail={}", messageId, tail(token));
            return messageId;
        } catch (Exception e) {
            // [추가 로그]
            log.warn("[FCM] Failed(one) tokenTail={} err={}", tail(token), e.toString(), e);
            throw new RuntimeException("FCM send failed", e);
        }
    }

    /**
     * 멀티캐스트. 실패 시(per 404 batch) 개별 전송으로 폴백
     */
    public FcmBatchResult sendToTokens(Collection<String> tokens, String title, String body) {
        return sendToTokens(tokens, title, body, Map.of());
    }

    public FcmBatchResult sendToTokens(Collection<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null) tokens = List.of();
        List<String> list = tokens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (list.isEmpty()) {
            // [추가 로그]
            log.info("[FCM] No tokens to send. title='{}'", title);
            return FcmBatchResult.empty();
        }

        // [추가 로그]
        if (log.isInfoEnabled()) {
            String tails = list.stream().map(FcmSenderService::tail).reduce((a, b) -> a + "," + b).orElse("");
            log.info("[FCM] Multicast try: total={}, tails=[{}], title='{}'", list.size(), tails, title);
        }

        // 1) 우선 멀티캐스트 시도
        try {
            MulticastMessage message = buildMulticastMessage(list, title, body, data);
            BatchResponse br = firebaseMessaging.sendEachForMulticast(message);

            int success = br.getSuccessCount();
            int failure = br.getFailureCount();
            // [추가 로그]
            log.info("[FCM] Multicast sent: total={}, success={}, failure={}", list.size(), success, failure);

            List<String> failedTokens = new ArrayList<>();
            for (int i = 0; i < br.getResponses().size(); i++) {
                SendResponse r = br.getResponses().get(i);
                if (!r.isSuccessful()) {
                    String tok = list.get(i);
                    failedTokens.add(tok);
                    String msg = r.getException() != null ? r.getException().getMessage() : "unknown";
                    // [추가 로그]
                    log.warn("[FCM] failed tokenTail={} err={}", tail(tok), msg);

                    // [선택] 무효 토큰 정리 예시
                    // if (msg != null && (msg.contains("NotRegistered") || msg.contains("InvalidRegistration"))) {
                    //     // userRepo나 별도 토큰 저장소에서 제거/NULL 업데이트
                    // }
                }
            }
            return new FcmBatchResult(list.size(), success, failure, failedTokens);
        } catch (FirebaseMessagingException e) {
            // 2) 배치 404면 개별 전송 폴백
            String msg = e.getMessage();
            if (msg != null && msg.contains("/batch") && msg.contains("404")) {
                // [추가 로그]
                log.warn("[FCM] batch 404 detected. Fallback to per-token send...");
                return fallbackSendIndividually(list, title, body, data);
            }
            // [추가 로그]
            log.warn("[FCM] Multicast send failed: {}", e.toString(), e);
            throw new RuntimeException("FCM multicast send failed", e);
        } catch (Exception e) {
            // [추가 로그]
            log.warn("[FCM] Multicast send failed: {}", e.toString(), e);
            throw new RuntimeException("FCM multicast send failed", e);
        }
    }

    private FcmBatchResult fallbackSendIndividually(List<String> tokens, String title, String body, Map<String, String> data) {
        int success = 0;
        List<String> failed = new ArrayList<>();
        for (String t : tokens) {
            try {
                sendToToken(t, title, body, data);
                success++;
            } catch (RuntimeException ex) {
                failed.add(t);
                // [선택] NotRegistered/InvalidRegistration 시 정리
            }
        }
        int total = tokens.size();
        int failure = total - success;
        // [추가 로그]
        log.info("[FCM] Fallback send(one-by-one) total={}, success={}, failure={}", total, success, failure);
        return new FcmBatchResult(total, success, failure, failed);
    }

    // ===== 도메인 헬퍼 =====
    public FcmBatchResult sendFollowing(Collection<String> targetTokens, String followerNickname, Long followerId) {
        String title = "새 팔로워";
        String body = followerNickname + " 님이 당신을 팔로우했습니다.";
        Map<String, String> data = Map.of(
                "type", "FOLLOWING",
                "followerId", String.valueOf(followerId),
                "followerNickname", followerNickname
        );
        return sendToTokens(targetTokens, title, body, data);
    }

    public void sendFollow(UserInfoEntity targetUser, String followerNickname, String followerProfilePhotoUrl, Long notificationId) {
        String title = "새 팔로워";
        String body = followerNickname + " 님이 당신을 팔로우했습니다.";
        Map<String, String> data = Map.of(
                "type", "FOLLOW",
                "notificationId", String.valueOf(notificationId),
                "actorNickname", followerNickname,
                "actorProfilePhotoUrl", followerProfilePhotoUrl != null ? followerProfilePhotoUrl : ""
        );
        List<String> tokens = resolveFcmTokens(targetUser);
        if (tokens.isEmpty()) {
            // [추가 로그]
            log.info("[FCM] No native tokens for target user (id={}). Skip sending FOLLOW.",
                    (targetUser != null ? String.valueOf(targetUser.getId()) : "null"));
            return;
        }
        sendToTokens(tokens, title, body, data);
    }

    // [신규 오버로드] 댓글 알림: 수신자 엔티티 입력받아 내부에서 토큰 해석
    public void sendComment(UserInfoEntity targetUser, String actorNickname, String preview, Long forumId, Long notificationId) {
        String title = "새 댓글";
        String body = actorNickname + " 님이 댓글을 남겼습니다.";
        Map<String, String> data = new HashMap<>();
        data.put("type", "COMMENT");
        if (forumId != null) data.put("forumId", String.valueOf(forumId));
        data.put("notificationId", String.valueOf(notificationId));
        data.put("actorNickname", actorNickname != null ? actorNickname : "");
        // [선택] 프리뷰를 data로도 전송할 수 있음(앱이 활용)
        if (preview != null && !preview.isBlank()) {
            data.put("preview", preview);
        }

        List<String> tokens = resolveFcmTokens(targetUser);
        if (tokens.isEmpty()) {
            // [추가 로그]
            log.info("[FCM] No native tokens for target user (id={}). Skip sending COMMENT.",
                    (targetUser != null ? String.valueOf(targetUser.getId()) : "null"));
            return;
        }
        sendToTokens(tokens, title, body, data);
    }

    public void sendFindNotification(UserInfoEntity targetUser, String body) {
        String title = "fin'd";

        List<String> tokens = resolveFcmTokens(targetUser);
        if (tokens.isEmpty()) {
            // [추가 로그]
            log.info("[FCM] No native tokens for target user (id={}). Skip sending FOLLOW.",
                    (targetUser != null ? String.valueOf(targetUser.getId()) : "null"));
            return;
        }
        sendToTokens(tokens, title, body, null);
    }

    public FcmBatchResult sendComment(Collection<String> targetTokens, String actorNickname, String postTitle, Long postId) {
        String title = "새 댓글";
        String body = actorNickname + " 님이 \"" + postTitle + "\"에 댓글을 달았습니다.";
        Map<String, String> data = Map.of(
                "type", "COMMENT",
                "postId", String.valueOf(postId),
                "actorNickname", actorNickname
        );
        return sendToTokens(targetTokens, title, body, data);
    }

    public FcmBatchResult sendLike(Collection<String> targetTokens, String actorNickname, Long postId) {
        String title = "새 좋아요";
        String body = actorNickname + " 님이 게시글을 좋아합니다.";
        Map<String, String> data = Map.of(
                "type", "LIKE",
                "postId", String.valueOf(postId),
                "actorNickname", actorNickname
        );
        return sendToTokens(targetTokens, title, body, data);
    }

    // ===== builders =====
    private Message buildMessage(String token, String title, String body, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Map.of() : data)
                .setAndroidConfig(androidDefaults())
                .setApnsConfig(apnsDefaults())
                .build();
    }

    private MulticastMessage buildMulticastMessage(Collection<String> tokens, String title, String body, Map<String, String> data) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Map.of() : data)
                .setAndroidConfig(androidDefaults())
                .setApnsConfig(apnsDefaults())
                .build();
    }

    private AndroidConfig androidDefaults() {
        return AndroidConfig.builder()
                .setTtl(Duration.ofHours(1).toMillis())
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();
    }

    private ApnsConfig apnsDefaults() {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setBadge(1)
                        .setContentAvailable(true)
                        .setSound("default")
                        .build())
                .setFcmOptions(ApnsFcmOptions.builder().setAnalyticsLabel("posthere_apns").build())
                .build();
    }

    // ===== 실제 토큰 조회 (A안: user_info.fcm_token 사용) =====
    protected List<String> resolveFcmTokens(UserInfoEntity targetUser) {
        if (targetUser == null) {
            // [추가 로그]
            log.info("[FCM] resolve tokens: targetUser is null");
            return List.of();
        }
        String t = targetUser.getFcmToken();
        if (t == null || t.isBlank()) {
            // [추가 로그]
            log.info("[FCM] resolve tokens: userId={} => NO_TOKEN", targetUser.getId());
            return List.of();
        }
        String token = t.trim();
        // [추가 로그]
        log.info("[FCM] resolve tokens: userId={} => 1 token (tail={})", targetUser.getId(), tail(token));
        return List.of(token);
    }

    // ===== DTO =====
    public record FcmBatchResult(int total, int success, int failure, List<String> failedTokens) {
        public static FcmBatchResult empty() {
            return new FcmBatchResult(0, 0, 0, List.of());
        }
    }
}
