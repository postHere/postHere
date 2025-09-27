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

    // ===================== [신규] 페이지 메타 =====================
    /**
     * hasNext:
     * - true  : 더 가져올 수 있는 알림이 남아있음
     * - false : 이번 응답이 마지막 페이지(무한 스크롤 종료)
     */
    private Boolean hasNext;
    // ============================================================

    // [변경] 기존 팩토리 보존(하위 호환), hasNext가 null인 응답을 만들 때 사용 가능
    public static NotificationListResponseDto of(List<NotificationItemResponseDto> items, long unreadCount) {
        return NotificationListResponseDto.builder()
                .items(items)
                .unreadCount(unreadCount)
                .hasNext(null) // [신규] 메타 제공이 없을 때는 null 유지 (프런트 휴리스틱 사용 가능)
                .build();
    }

    // [신규] hasNext 포함 팩토리
    public static NotificationListResponseDto of(List<NotificationItemResponseDto> items, long unreadCount, boolean hasNext) {
        return NotificationListResponseDto.builder()
                .items(items)
                .unreadCount(unreadCount)
                .hasNext(hasNext)
                .build();
    }
}
