// src/main/java/io/github/nokasegu/post_here/notification/dto/NotificationItemResponseDto.java
package io.github.nokasegu.post_here.notification.dto;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationItemResponseDto {

    private Long id;                 // 알림 PK
    private String type;             // "FOLLOW" | "FORUM_COMMENT" | "COMMENT" | "FIND_FOUND" 등
    private String text;             // 고정 문구(프론트 mapText와 동일 정책)
    private boolean isRead;          // 읽음 여부
    private LocalDateTime createdAt; // 생성 시각

    // 프론트에서 사용: actor.nickname / actor.profilePhotoUrl
    private Map<String, Object> actor;

    // 프론트에서 사용: 댓글 미리보기(선택)
    private String commentPreview;

    // 프론트에서 사용: 클릭 이동용 링크
    private String link;

    /**
     * 기존 from(NotificationEntity) 관례를 유지하되, link/preview 등은 이후 보강할 수 있도록 구성
     */
    public static NotificationItemResponseDto from(NotificationEntity n) {
        NotificationCode code = n.getNotificationCode();

        Map<String, Object> actor = new HashMap<>();
        if (code == NotificationCode.FOLLOW && n.getFollowing() != null) {
            FollowingEntity f = n.getFollowing();
            UserInfoEntity follower = f.getFollower();
            if (follower != null) {
                if (follower.getNickname() != null) actor.put("nickname", follower.getNickname());
                if (follower.getProfilePhotoUrl() != null) actor.put("profilePhotoUrl", follower.getProfilePhotoUrl());
            }
        } else if (code == NotificationCode.COMMENT && n.getComment() != null) {
            UserInfoEntity writer = n.getComment().getWriter();
            if (writer != null) {
                if (writer.getNickname() != null) actor.put("nickname", writer.getNickname());
                if (writer.getProfilePhotoUrl() != null) actor.put("profilePhotoUrl", writer.getProfilePhotoUrl());
            }
        }

        String typeStr = switch (code) {
            case FOLLOW -> "FOLLOW";
            case COMMENT -> "FORUM_COMMENT"; // 프론트에서 COMMENT/ FORUM_COMMENT를 같이 처리하므로 FORUM_COMMENT로 명시
            case FIND_FOUND -> "FIND_FOUND";
            default -> "ALERT";
        };

        String text = switch (code) {
            case FOLLOW -> "회원님을 팔로우하기 시작했습니다";
            case COMMENT -> "회원님의 Forum에 댓글을 남겼습니다";
            case FIND_FOUND -> "습득물이 발견되었습니다.";
            default -> "알림";
        };

        return NotificationItemResponseDto.builder()
                .id(n.getId())
                .type(typeStr)
                .text(text)
                .isRead(Boolean.TRUE.equals(n.getCheckStatus()))
                .createdAt(n.getCreatedAt())
                .actor(actor.isEmpty() ? null : actor)
                .build();
    }
}
