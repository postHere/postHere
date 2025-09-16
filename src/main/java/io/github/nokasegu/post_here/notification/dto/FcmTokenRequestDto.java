package io.github.nokasegu.post_here.notification.dto;

import io.github.nokasegu.post_here.notification.domain.FcmTokenEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenRequestDto {
    private String token;     // FCM registration token (필수)
    private String platform;  // 예: "android"
    private String app;       // 예: "post-here"

    public static FcmTokenRequestDto convertToDto(FcmTokenEntity e) {
        return FcmTokenRequestDto.builder()
                .token(e.getToken())
                .platform(e.getPlatform())
                .app(e.getApp())
                .build();
    }

    public FcmTokenEntity convertToEntity(UserInfoEntity user) {
        return FcmTokenEntity.builder()
                .user(user)
                .token(token)
                .platform(platform)
                .app(app)
                .build();
    }
}
