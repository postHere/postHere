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

/**
 * NotificationService
 * <p>
 * 역할
 * - 알림 생성/목록/읽음 처리/미읽음 카운트
 * - Web Push + FCM 동시 발사 연동(createFollowAndPush)
 * <p>
 * 주의
 * - 기존 주석/구조는 유지하고 필요한 최소 변경만 반영
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository; // 알림 CRUD/배치 읽음처리
    private final UserInfoRepository userInfoRepository;         // 타겟 유저 검증/조회
    private final WebPushService webPushService;                 // Web Push 전송(브라우저 구독 대상)
    private final FcmSenderService fcmSenderService;             // FCM(Android) 전송

    /**
     * 팔로우 발생 시 알림 생성 + WebPush/FCM 동시 발사
     */
    @Transactional
    public NotificationEntity createFollowAndPush(FollowingEntity following) {
        NotificationEntity n = NotificationEntity.builder()
                .notificationCode(NotificationCode.FOLLOW)
                .following(following)
                .targetUser(following.getFollowed())
                .checkStatus(false)
                .build();
        NotificationEntity saved = notificationRepository.save(n);

        // Web Push payload
        Map<String, Object> payload = Map.of(
                "type", "FOLLOW",
                "notificationId", saved.getId(),
                "actor", Map.of(
                        "nickname", following.getFollower().getNickname(),
                        "profilePhotoUrl", following.getFollower().getProfilePhotoUrl()
                ),
                "text", "Started following you"
        );

        // 웹 푸시
        webPushService.sendToUser(following.getFollowed(), payload);

        // FCM(Android)
        fcmSenderService.sendFollow(
                following.getFollowed(),
                following.getFollower().getNickname(),
                following.getFollower().getProfilePhotoUrl(),
                saved.getId()
        );

        return saved;
    }

    /**
     * 알림 목록 + 미읽음 카운트
     */
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

    /**
     * 선택 읽음 처리
     */
    @Transactional
    public int markRead(Long targetUserId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;

        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return notificationRepository.markReadByIds(target, notificationIds);
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public int markAllRead(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return notificationRepository.markAllRead(target);
    }

    /**
     * 미읽음 카운트
     */
    @Transactional(readOnly = true)
    public long unreadCount(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }
}
