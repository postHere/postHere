package io.github.nokasegu.post_here.notification.dto;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationActorDto {
    private Long userId;
    private String nickname;
    private String profilePhotoUrl;

    public static NotificationActorDto of(UserInfoEntity u) {
        return NotificationActorDto.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profilePhotoUrl(u.getProfilePhotoUrl())
                .build();
    }
}
