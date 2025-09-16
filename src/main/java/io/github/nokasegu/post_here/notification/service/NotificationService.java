import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.notification.dto.NotificationItemResponseDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListResponseDto;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Transactional(readOnly = true)
public NotificationListResponseDto list(Long targetUserId, int page, int size) {
    UserInfoEntity target = userInfoRepository.findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

    List<NotificationEntity> entities = notificationRepository.findListByTarget(target, PageRequest.of(page, size));
    List<NotificationItemResponseDto> items = entities.stream()
            .map(NotificationItemResponseDto::from)
            .toList();

    long unreadCount = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);

    return NotificationListResponseDto.of(items, unreadCount);
}
