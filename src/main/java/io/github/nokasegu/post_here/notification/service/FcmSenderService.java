package io.github.nokasegu.post_here.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * FcmSenderService
 * - user_info.fcm_token 단일 컬럼 기반으로 FCM 발송 (Firebase Admin SDK 사용)
 * - Android 우선 구성 (iOS 필요 시 ApnsConfig 추가)
 * - 팔로우/댓글/좋아요 전용 헬퍼 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmSenderService {

    // 단일 토큰 조회를 위해 JdbcTemplate 사용
    private final JdbcTemplate jdbcTemplate;

    /**
     * DB에서 대상 사용자 fcm_token 조회
     */
    private String resolveToken(Long userId) {
        try {
            return jdbcTemplate.query(
                    "SELECT fcm_token FROM user_info WHERE user_info_pk = ?",
                    ps -> ps.setLong(1, userId),
                    rs -> rs.next() ? rs.getString(1) : null
            );
        } catch (Exception e) {
            log.warn("[FCM] token lookup failed userId={}", userId, e);
            return null;
        }
    }

    /**
     * 공통 발송 유틸
     */
    public void sendToUser(UserInfoEntity target, String title, String body, Map<String, String> data) {
        if (target == null || target.getId() == null) {
            log.debug("[FCM] invalid target");
            return;
        }
        String token = resolveToken(target.getId());
        if (token == null || token.isBlank()) {
            log.debug("[FCM] no token for user: {}", target.getId());
            return;
        }

        try {
            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setTtl(Duration.ofHours(1).toMillis())
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .build());

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            String msgId = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("[FCM] sent messageId={} to userId={}", msgId, target.getId());
        } catch (Exception e) {
            log.warn("[FCM] send fail userId={}, token={}", target.getId(), token, e);
        }
    }

    /**
     * 팔로우 알림
     */
    public void sendFollow(UserInfoEntity target, String actorNickname, String actorProfileUrl, Long notificationId) {
        String title = "새 팔로우";
        String body = (actorNickname != null ? actorNickname : "누군가") + "님이 팔로우했습니다";

        Map<String, String> data = new HashMap<>();
        data.put("type", "FOLLOW");
        data.put("notificationId", String.valueOf(notificationId));
        if (actorNickname != null) data.put("actorNickname", actorNickname);
        if (actorProfileUrl != null) data.put("actorProfileUrl", actorProfileUrl);
        data.put("deeplink", "posthere://notification/follow"); // 앱에서 처리할 딥링크

        sendToUser(target, title, body, data);
    }

    /**
     * 댓글 알림
     */
    public void sendComment(UserInfoEntity target, String actorNickname, String commentText, Long notificationId) {
        String title = "새 댓글";
        String body = (actorNickname != null ? actorNickname : "누군가") + "님이 댓글을 남겼습니다"
                + (commentText != null ? (": " + commentText) : "");

        Map<String, String> data = new HashMap<>();
        data.put("type", "COMMENT");
        data.put("notificationId", String.valueOf(notificationId));
        if (actorNickname != null) data.put("actorNickname", actorNickname);
        if (commentText != null) data.put("commentText", commentText);
        data.put("deeplink", "posthere://notification/comment");

        sendToUser(target, title, body, data);
    }

    /**
     * 좋아요 알림
     */
    public void sendLike(UserInfoEntity target, String actorNickname, Long notificationId) {
        String title = "새 좋아요";
        String body = (actorNickname != null ? actorNickname : "누군가") + "님이 회원님의 글을 좋아합니다";

        Map<String, String> data = new HashMap<>();
        data.put("type", "LIKE");
        data.put("notificationId", String.valueOf(notificationId));
        if (actorNickname != null) data.put("actorNickname", actorNickname);
        data.put("deeplink", "posthere://notification/like");

        sendToUser(target, title, body, data);
    }
}
