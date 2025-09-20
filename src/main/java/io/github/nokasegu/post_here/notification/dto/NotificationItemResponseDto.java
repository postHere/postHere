// src/main/java/io/github/nokasegu/post_here/notification/dto/NotificationItemResponseDto.java
package io.github.nokasegu.post_here.notification.dto;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationItemResponseDto {
    private Long id;
    private NotificationCode type;
    private NotificationActorDto actor;   // ← 프런트는 actor.nickname을 사용
    private String text;
    private LocalDateTime createdAt;
    private boolean isRead;

    public static NotificationItemResponseDto from(NotificationEntity e) {
        FollowingEntity f = e.getFollowing();               // FOLLOW 전용 리스트라 가정
        UserInfoEntity follower = (f != null) ? f.getFollower() : null;

        return NotificationItemResponseDto.builder()
                .id(e.getId())
                .type(e.getNotificationCode())              // "FOLLOW"
                .actor(NotificationActorDto.of(follower))   // ← 닉네임/대체값까지 보장
                .text("Started following you")
                .createdAt(e.getCreatedAt())
                .isRead(Boolean.TRUE.equals(e.getCheckStatus()))
                .build();
    }
}
