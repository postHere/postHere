// src/main/java/io/github/nokasegu/post_here/notification/dto/NotificationActorDto.java
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

    private Long id;
    /**
     * 화면 표시용 이름: 닉네임 없으면 email 아이디/또는 User#ID 로 대체
     */
    private String nickname;
    private String profilePhotoUrl;

    public static NotificationActorDto of(UserInfoEntity u) {
        if (u == null) return null;

        String display = u.getNickname();
        if (display == null || display.isBlank()) {
            String email = u.getEmail();
            if (email != null && !email.isBlank()) {
                int at = email.indexOf('@');
                display = (at > 0) ? email.substring(0, at) : email;
            } else {
                display = "User#" + u.getId();
            }
        }

        return NotificationActorDto.builder()
                .id(u.getId())
                .nickname(display)
                .profilePhotoUrl(u.getProfilePhotoUrl())
                .build();
    }
}
