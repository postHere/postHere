package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.notification.dto.NotificationItemResponseDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListResponseDto;
import io.github.nokasegu.post_here.notification.repository.NotificationRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository; // 알림 CRUD/배치 읽음처리
    private final UserInfoRepository userInfoRepository;         // 타겟 유저 검증/조회
    private final WebPushService webPushService;                 // Web Push 전송(브라우저 구독 대상)
    private final FcmSenderService fcmSenderService;             // 네이티브(Firebase FCM) 발송자

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

        fcmSenderService.sendFollow(
                following.getFollowed(),
                following.getFollower().getNickname(),
                following.getFollower().getProfilePhotoUrl(),
                saved.getId()
        );

        return saved;
    }

    // ========================
    // 📌 APIController 매칭 메서드들
    // ========================

    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        List<NotificationEntity> entities = notificationRepository.findListByTarget(target, PageRequest.of(page, size));
        List<NotificationItemResponseDto> items = entities.stream()
                .map(NotificationItemResponseDto::from)
                .toList();
        return new NotificationListResponseDto(items);
    }

    @Transactional
    public long markRead(Long targetUserId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return notificationRepository.markReadByIds(target, ids);
    }

    @Transactional
    public long markAllRead(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return notificationRepository.markAllRead(target);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }
}
