// src/main/java/io/github/nokasegu/post_here/notification/service/NotificationService.java
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserInfoRepository userInfoRepository;
    private final WebPushService webPushService;
    private final FcmSenderService fcmSenderService;

    /**
     * AFTER_COMMIT 리스너에서 호출되는 알림 생성/전송 메서드.
     * 기존 트랜잭션이 이미 끝난 뒤 실행되므로 "새 트랜잭션"을 강제로 시작한다.
     * (transactionManager 이름은 JPA TM 빈 이름에 맞게 필요 시 변경)
     */
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW)
    public NotificationEntity createFollowAndPush(FollowingEntity following) {
        log.info("[NOTI] createFollowAndPush START");
        log.info("[NOTI] before save");

        NotificationEntity n = NotificationEntity.builder()
                .notificationCode(NotificationCode.FOLLOW)
                .following(following)
                .targetUser(following.getFollowed())
                .checkStatus(false)
                .build();

        // PK 바로 채우도록 flush까지 보장
        NotificationEntity saved = notificationRepository.saveAndFlush(n);
        log.info("[NOTI] after save id={}", saved.getId());

        // Web Push
        log.info("[NOTI] webPush send start");
        webPushService.sendToUser(following.getFollowed(), buildFollowPayload(saved, following));
        log.info("[NOTI] webPush send end");

        // FCM
        log.info("[NOTI] fcm send start");
        fcmSenderService.sendFollow(
                following.getFollowed(),
                following.getFollower().getNickname(),
                following.getFollower().getProfilePhotoUrl(),
                saved.getId()
        );
        log.info("[NOTI] fcm send end");

        log.info("[NOTI] createFollowAndPush END id={}", saved.getId());
        return saved;
    }

    private Map<String, Object> buildFollowPayload(NotificationEntity saved, FollowingEntity following) {
        Map<String, Object> actor = new HashMap<>();
        if (following.getFollower() != null) {
            if (following.getFollower().getNickname() != null) {
                actor.put("nickname", following.getFollower().getNickname());
            }
            if (following.getFollower().getProfilePhotoUrl() != null) {
                actor.put("profilePhotoUrl", following.getFollower().getProfilePhotoUrl());
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "FOLLOW");
        payload.put("notificationId", saved.getId());
        payload.put("actor", actor);
        payload.put("text", "Started following you");
        return payload;
    }

    // ===== 리스트/읽음 관련 =====

    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        List<NotificationEntity> entities =
                notificationRepository.findListByTarget(target, PageRequest.of(page, size));

        List<NotificationItemResponseDto> items = entities.stream()
                .map(NotificationItemResponseDto::from)
                .toList();

        long unreadCount = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);

        return NotificationListResponseDto.of(items, unreadCount);
    }

    @Transactional(transactionManager = "transactionManager")
    public int markRead(Long targetUserId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;

        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return notificationRepository.markReadByIds(target, notificationIds);
    }

    @Transactional(transactionManager = "transactionManager")
    public int markAllRead(Long targetUserId) {
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
