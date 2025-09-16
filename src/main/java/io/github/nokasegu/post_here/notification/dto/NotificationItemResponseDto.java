package io.github.nokasegu.post_here.notification.dto;

import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
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
    private NotificationActorDto actor;
    private String text;
    private LocalDateTime createdAt;
    private boolean isRead;

    public static NotificationItemResponseDto from(NotificationEntity e) {
        return NotificationItemResponseDto.builder()
                .id(e.getId())
                .type(e.getNotificationCode())
                .actor(NotificationActorDto.of(e.getFollowing().getFollower()))
                .text("Started following you")
                .createdAt(e.getCreatedAt())
                .isRead(Boolean.TRUE.equals(e.getCheckStatus()))
                .build();
    }
}
