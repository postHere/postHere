package io.github.nokasegu.post_here.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 업로드 요청 DTO
 * - Capacitor setupPush()에서 전송
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenRequestDto {
    private String token;
    private String platform; // "android" 등
    private String app;      // "post-here"
}
