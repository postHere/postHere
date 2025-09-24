// src/main/java/io/github/nokasegu/post_here/notification/service/FcmSenderService.java
package io.github.nokasegu.post_here.notification.service;

import com.google.firebase.messaging.*;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FirebaseApp/FirebaseMessaging을 정적 호출(getInstance)로 직접 잡지 않고
 * Spring 빈 주입으로만 사용해 초기화 순서 문제를 제거합니다.
 * <p>
 * - 단건 전송: sendToToken(...)
 * - 다건 전송: sendToTokens(...)
 * - 도메인별 헬퍼: sendFollowing/sendComment/sendLike (필요 시 사용)
 * <p>
 * 주의:
 * 1) 반드시 FirebaseConfig에서 FirebaseMessaging 빈이 등록되어 있어야 합니다.
 * 2) title/body는 notification 페이로드로 포함되고, data는 앱에서 onMessageReceived로 처리할 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmSenderService {

    private final FirebaseMessaging firebaseMessaging; // ✅ 주입 방식

    // ===== Public API =====

    private static String tail(String token) {
        if (token == null) return "null";
        int n = token.length();
        return n <= 6 ? token : token.substring(n - 6);
    }

    /**
     * 단일 토큰으로 알림 전송
     */
    // [선택-경고억제] 일부 Firebase API에서 deprecated 경고가 뜰 수 있어 경고만 억제합니다. 기능 변화는 없습니다.
    @SuppressWarnings("deprecation")
    public String sendToToken(String token, String title, String body) {
        return sendToToken(token, title, body, Map.of());
    }

    /**
     * 단일 토큰 + data 전송
     */
    // [선택-경고억제] 위와 동일한 이유로 경고 억제
    @SuppressWarnings("deprecation")
    public String sendToToken(String token, String title, String body, Map<String, String> data) {
        Objects.requireNonNull(token, "token must not be null");
        try {
            Message message = buildMessage(token, title, body, data);
            String messageId = firebaseMessaging.send(message);
            log.info("[FCM] Sent to one token: messageId={}, tokenTail={}", messageId, tail(token));
            return messageId;
        } catch (Exception e) {
            log.error("[FCM] Failed to send to tokenTail={} : {}", tail(token), e.toString(), e);
            throw new RuntimeException("FCM send failed", e);
        }
    }

    /**
     * 여러 토큰으로 알림 전송 (최대 500개/회)
     */
    // [선택-경고억제] 위와 동일한 이유로 경고 억제
    @SuppressWarnings("deprecation")
    public FcmBatchResult sendToTokens(Collection<String> tokens, String title, String body) {
        return sendToTokens(tokens, title, body, Map.of());
    }

    // ===== Domain helpers (선택 사용) =====

    /**
     * 여러 토큰 + data 전송
     */
    // [선택-경고억제] 위와 동일한 이유로 경고 억제 (sendMulticast 사용부)
    @SuppressWarnings("deprecation")
    public FcmBatchResult sendToTokens(Collection<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.info("[FCM] No tokens to send.");
            return FcmBatchResult.empty();
        }
        try {
            MulticastMessage message = buildMulticastMessage(tokens, title, body, data);
            var br = firebaseMessaging.sendMulticast(message);

            int success = br.getSuccessCount();
            int failure = br.getFailureCount();
            log.info("[FCM] Multicast sent: total={}, success={}, failure={}", tokens.size(), success, failure);

            // 실패 토큰 수집(로깅/정리용)
            var failedTokens = new java.util.ArrayList<String>();
            List<SendResponse> responses = br.getResponses();
            int idx = 0;
            for (SendResponse r : responses) {
                if (!r.isSuccessful()) {
                    failedTokens.add(((List<String>) tokens).get(idx));
                    log.warn("[FCM] failed tokenTail={}, err={}", tail(((List<String>) tokens).get(idx)),
                            r.getException() != null ? r.getException().getMessage() : "unknown");
                }
                idx++;
            }
            return new FcmBatchResult(tokens.size(), success, failure, failedTokens);
        } catch (Exception e) {
            log.error("[FCM] Multicast send failed: {}", e.toString(), e);
            throw new RuntimeException("FCM multicast send failed", e);
        }
    }

    /**
     * 팔로우 알림 도메인 헬퍼 (필요하면 호출부에서 토큰 조회 후 넘겨 사용)
     */
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

    // [추가] NotificationService에서 사용하는 시그니처를 그대로 지원하는 오버로드 메서드
    // - 호출부(NotificationService#createFollowAndPush)와 시그니처를 맞춰 "cannot find symbol" 컴파일 에러를 제거합니다.
    // - targetUser로부터 FCM 토큰들을 조회하여 기존 멀티캐스트 전송 로직(sendToTokens)으로 위임합니다.
    public void sendFollow(UserInfoEntity targetUser,
                           String followerNickname,
                           String followerProfilePhotoUrl,
                           Long notificationId) {

        String title = "새 팔로워";
        String body = followerNickname + " 님이 당신을 팔로우했습니다.";

        Map<String, String> data = Map.of(
                "type", "FOLLOW",
                "notificationId", String.valueOf(notificationId),
                "actorNickname", followerNickname,
                "actorProfilePhotoUrl", followerProfilePhotoUrl != null ? followerProfilePhotoUrl : ""
        );

        // [중요] 실제 구현에서는 targetUser에 연동된 "네이티브 앱 FCM 토큰"을 조회해야 합니다.
        //       현재는 빌드가 깨지지 않도록 기본 구현(빈 리스트/스킵)을 제공하며,
        //       아래 resolveFcmTokens(...) 안에 프로젝트의 실제 토큰 저장소 접근 로직을 작성하세요.
        List<String> tokens = resolveFcmTokens(targetUser);

        if (tokens == null || tokens.isEmpty()) {
            // [수정] SLF4J 오버로드 모호성 방지: null → Throwable로 해석되는 문제 방지를 위해 문자열 변환 사용
            //        또한 UserInfoEntity의 PK getter는 getId()이므로 그에 맞춰 로깅합니다.
            log.info("[FCM] No native tokens for target user (id={}). Skip sending FOLLOW.",
                    (targetUser != null && targetUser.getId() != null) ? String.valueOf(targetUser.getId()) : "null");
            return; // 토큰이 없으면 전송 스킵 (에러 아님)
        }

        sendToTokens(tokens, title, body, data);
    }

    /**
     * 댓글 알림 도메인 헬퍼
     */
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

    // ===== Builders =====

    /**
     * 좋아요 알림 도메인 헬퍼
     */
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

    private Message buildMessage(String token, String title, String body, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data == null ? Map.of() : data)
                .setAndroidConfig(androidDefaults())
                .setApnsConfig(apnsDefaults())
                .build();
    }

    private MulticastMessage buildMulticastMessage(Collection<String> tokens, String title, String body, Map<String, String> data) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data == null ? Map.of() : data)
                .setAndroidConfig(androidDefaults())
                .setApnsConfig(apnsDefaults())
                .build();
    }

    private AndroidConfig androidDefaults() {
        // 필요 시 채널ID/TTL 등 지정
        return AndroidConfig.builder()
                .setTtl(Duration.ofHours(1).toMillis())
                .setPriority(AndroidConfig.Priority.HIGH)
                // .setNotification(AndroidNotification.builder().setChannelId("posthere_default").build())
                .build();
    }

    private ApnsConfig apnsDefaults() {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setBadge(1)
                        .setContentAvailable(true)
                        .setSound("default")
                        .build())
                .setFcmOptions(ApnsFcmOptions.builder()
                        .setAnalyticsLabel("posthere_apns")
                        .build())
                .build();
    }

    // ===== 실제 토큰 조회 연결 지점 =====
    // [추가] 프로젝트의 실제 스키마/저장소에 맞게 FCM 토큰 조회 로직을 구현하세요.
    //       현재는 예비 구현으로 빈 리스트를 반환하여 "토큰 없음 → 전송 스킵" 동작을 하게 합니다.
    protected List<String> resolveFcmTokens(UserInfoEntity targetUser) {
        // TODO: 예)
        // return fcmTokenRepository.findAllByUser(targetUser).stream()
        //                          .map(FcmTokenEntity::getToken)
        //                          .toList();
        return List.of();
    }

    // ===== DTO =====

    public record FcmBatchResult(int total, int success, int failure, List<String> failedTokens) {
        public static FcmBatchResult empty() {
            return new FcmBatchResult(0, 0, 0, List.of());
        }
    }
}
