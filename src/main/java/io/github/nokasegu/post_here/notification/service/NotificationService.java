package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.notification.repository.NotificationRepository;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository; // 알림 CRUD/배치 읽음처리
    private final UserInfoRepository userInfoRepository;         // 타겟 유저 검증/조회
    private final WebPushService webPushService;                 // Web Push 전송(브라우저 구독 대상)
    // ✅ [추가] 네이티브(Firebase FCM) 발송자
    private final FcmSenderService fcmSenderService;

    @Transactional
    public NotificationEntity createFollowAndPush(FollowingEntity following) {
        NotificationEntity n = NotificationEntity.builder()
                .notificationCode(NotificationCode.FOLLOW)
                .following(following)
                .targetUser(following.getFollowed())
                .checkStatus(false)
                .build();
        NotificationEntity saved = notificationRepository.save(n);

        Map<String, Object> payload = Map.of(
                "type", "FOLLOW",
                "notificationId", saved.getId(),
                "actor", Map.of(
                        "nickname", following.getFollower().getNickname(),
                        "profilePhotoUrl", following.getFollower().getProfilePhotoUrl()
                ),
                "text", "Started following you"
        );

        webPushService.sendToUser(following.getFollowed(), payload);

        // ✅ [추가] 네이티브(Android)로도 동시 발송
        fcmSenderService.sendFollow(
                following.getFollowed(),
                following.getFollower().getNickname(),
                following.getFollower().getProfilePhotoUrl(),
                saved.getId()
        );

        return saved;
    }

    // ... (이하 기존 코드 그대로 유지)
}
