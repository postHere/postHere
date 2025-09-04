package io.github.nokasegu.post_here.userInfo.dto;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoDto {

    private String nickname;
    private String email;
    private String profilePhotoUrl;

    /**
     * ✅ [수정] 잘못된 메소드를 올바른 생성자로 변경합니다.
     * UserInfoEntity를 받아 UserInfoDto를 생성합니다.
     *
     * @param user UserInfoEntity 객체
     */
    public UserInfoDto(UserInfoEntity user) {
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.profilePhotoUrl = user.getProfilePhotoUrl() != null
                ? user.getProfilePhotoUrl()
                : "https://placehold.co/112x112/E2E8F0/4A5568?text=User"; // 기본 이미지
    }
}
