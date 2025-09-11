package io.github.nokasegu.post_here.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponseDto {
    private List<NotificationItemResponseDto> items;
    private long unreadCount;

    public static NotificationListResponseDto of(List<NotificationItemResponseDto> items, long unreadCount) {
        return NotificationListResponseDto.builder()
                .items(items)
                .unreadCount(unreadCount)
                .build();
    }
}
