package io.github.nokasegu.post_here.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListRequestDto {
    private Integer page;
    private Integer size;

    public int safePage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int safeSize() {
        return size == null || size <= 0 ? 20 : size;
    }
}
