// src/main/java/io/github/nokasegu/post_here/notification/service/FcmSenderService.java
package io.github.nokasegu.post_here.notification.service;

import com.google.firebase.messaging.*;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

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
            log.info("[FCM] Sent(one) id={}, tokenTail={}", messageId, tail(token));
            return messageId;
        } catch (Exception e) {
            log.error("[FCM] Failed(one) tokenTail={} err={}", tail(token), e.toString(), e);
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
            log.info("[FCM] No tokens to send.");
            return FcmBatchResult.empty();
        }

        // 1) 우선 멀티캐스트 시도
        try {
            MulticastMessage message = buildMulticastMessage(list, title, body, data);
            BatchResponse br = firebaseMessaging.sendMulticast(message);

            int success = br.getSuccessCount();
            int failure = br.getFailureCount();
            log.info("[FCM] Multicast sent: total={}, success={}, failure={}", list.size(), success, failure);

            List<String> failedTokens = new ArrayList<>();
            for (int i = 0; i < br.getResponses().size(); i++) {
                SendResponse r = br.getResponses().get(i);
                if (!r.isSuccessful()) {
                    String tok = list.get(i);
                    failedTokens.add(tok);
                    String msg = r.getException() != null ? r.getException().getMessage() : "unknown";
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
                log.warn("[FCM] batch 404 detected. Fallback to per-token send...");
                return fallbackSendIndividually(list, title, body, data);
            }
            log.error("[FCM] Multicast send failed: {}", e.toString(), e);
            throw new RuntimeException("FCM multicast send failed", e);
        } catch (Exception e) {
            log.error("[FCM] Multicast send failed: {}", e.toString(), e);
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
            log.info("[FCM] No native tokens for target user (id={}). Skip sending FOLLOW.",
                    targetUser != null ? String.valueOf(targetUser.getId()) : "null");
            return;
        }
        sendToTokens(tokens, title, body, data);
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
        if (targetUser == null) return List.of();
        String t = targetUser.getFcmToken();
        if (t == null || t.isBlank()) return List.of();
        return List.of(t.trim());
    }

    // ===== DTO =====
    public record FcmBatchResult(int total, int success, int failure, List<String> failedTokens) {
        public static FcmBatchResult empty() {
            return new FcmBatchResult(0, 0, 0, List.of());
        }
    }
}
