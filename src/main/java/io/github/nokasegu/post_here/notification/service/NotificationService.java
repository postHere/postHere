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

    private final NotificationRepository notificationRepository;
    private final UserInfoRepository userInfoRepository;
    private final WebPushService webPushService;

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

        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        var list = notificationRepository.findListByTarget(target, PageRequest.of(page, size));
        long unread = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
        var items = list.stream().map(NotificationItemResponseDto::fromEntity).toList();
        return NotificationListResponseDto.of(items, unread);
    }

    @Transactional
    public long markRead(Long targetUserId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return unreadCount(targetUserId);
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        notificationRepository.markReadByIds(target, ids);
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }

    @Transactional
    public long markAllRead(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        notificationRepository.markAllRead(target);
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }
}
