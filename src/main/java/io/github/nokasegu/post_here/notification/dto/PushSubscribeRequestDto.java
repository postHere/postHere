package io.github.nokasegu.post_here.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscribeRequestDto {
    private String endpoint;
    private Keys keys;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
