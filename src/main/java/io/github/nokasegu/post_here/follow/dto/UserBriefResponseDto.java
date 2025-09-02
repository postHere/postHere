package io.github.nokasegu.post_here.follow.dto;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBriefResponseDto {

    private Long id;
    private String nickname;
    private String profilePhotoUrl;

    public static UserBriefResponseDto convertToDto(UserInfoEntity e) {
        return UserBriefResponseDto.builder()
                .id(e.getId())
                .nickname(e.getNickname())
                .profilePhotoUrl(e.getProfilePhotoUrl())
                .build();
    }

    public static UserInfoEntity convertToEntity(UserBriefResponseDto d) {
        return null;
    }
}

