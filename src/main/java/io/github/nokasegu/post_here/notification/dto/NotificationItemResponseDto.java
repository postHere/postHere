// src/main/java/io/github/nokasegu/post_here/notification/dto/NotificationItemResponseDto.java
package io.github.nokasegu.post_here.notification.dto;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationItemResponseDto {
    private Long id;
    private NotificationCode type;
    private NotificationActorDto actor;   // ← 프런트는 actor.nickname을 사용
    private String text;
    private LocalDateTime createdAt;
    private boolean isRead;

    // ====================== [추가 필드] ======================
    // - 댓글 알림(COMMENT)에서 목록에 "댓글 미리보기"를 2줄로 보여주기 위한 원본 텍스트(줄바꿈/공백 정돈 + 길이 제한 적용)
    // - FOLLOW 등 다른 타입에서는 null 유지 (프런트는 존재할 때만 그려주면 됨)
    private String commentPreview;

    // - 알림 클릭 시 이동할 링크. 요구사항: forum 상세는 반드시 GET /forum/{forumId}
    // - FOLLOW 등에서는 null 가능 (프런트에서 기존 규칙대로 프로필로 이동하는 로직 유지)
    private String link;
    // =======================================================

    /**
     * [기존 주석/의도 유지]
     * <p>
     * - FOLLOW 전용으로 작성되어 있었던 부분을 보존하면서,
     * NotificationCode 기준으로 FOLLOW / COMMENT를 분기 지원하도록 확장.
     * <p>
     * - 변경점 요약
     * 1) type 분기 추가: FOLLOW(기존 로직), COMMENT(댓글 작성자/미리보기/링크 구성)
     * 2) commentPreview, link 필드 추가 (기존 프론트와의 호환성 깨지지 않도록 선택 필드로 설계)
     * 3) 기존 필드/명세/주석은 삭제/수정하지 않고 유지
     */
    public static NotificationItemResponseDto from(NotificationEntity e) {
        final NotificationCode code = e.getNotificationCode();

        // 공통 기본값 세팅용 빌더
        NotificationItemResponseDtoBuilder b = NotificationItemResponseDto.builder()
                .id(e.getId())
                .type(code)
                .createdAt(e.getCreatedAt())
                .isRead(Boolean.TRUE.equals(e.getCheckStatus()));

        // ===================== [분기: FOLLOW] =====================
        if (code == NotificationCode.FOLLOW) {
            FollowingEntity f = e.getFollowing();               // FOLLOW 전용 리스트라 가정
            UserInfoEntity follower = (f != null) ? f.getFollower() : null;

            return b.actor(NotificationActorDto.of(follower))   // ← 닉네임/대체값까지 보장
                    .text("Started following you")
                    // FOLLOW는 목록 클릭 시 프로필로 가는 기존 프런트 로직을 유지하므로 link/commentPreview는 설정하지 않음
                    .build();
        }

        // ===================== [분기: COMMENT] =====================
        if (code == NotificationCode.COMMENT) {
            // 안전하게 null 체크
            ForumCommentEntity c = e.getComment();
            UserInfoEntity actorUser = (c != null) ? c.getWriter() : null;

            // 알림 문구: "<닉네임> 님이 댓글을 남겼습니다"
            String actorNick = (actorUser != null && actorUser.getNickname() != null)
                    ? actorUser.getNickname()
                    : "Someone";
            String text = actorNick + " 님이 댓글을 남겼습니다";

            // 댓글 미리보기(2줄 클램프 전제)용 백엔드 정리:
            //  - 줄바꿈/연속 공백 정리
            //  - 길이 제한(대략 140자) 후 말줄임표 (…) 부여
            //  - 실제 2줄 렌더링은 프런트 CSS(line-clamp)로 처리, 서버는 1차 안전 가공만 수행
            String previewSrc = (c != null && c.getContentsText() != null) ? c.getContentsText() : "";
            String preview = ellipsize(squashWhitespace(previewSrc), 140);

            // 링크 규칙(요구사항 고정): 반드시 GET /forum/{forumId}
            Long forumId = (c != null && c.getForum() != null) ? c.getForum().getId() : null;
            String link = (forumId != null) ? ("/forum/" + forumId) : null;

            return b.actor(NotificationActorDto.of(actorUser))
                    .text(text)
                    .commentPreview(preview)
                    .link(link)
                    .build();
        }

        // ===================== [기타 타입 기본 처리] =====================
        // [변경] 기존에 NotificationActorDto.empty()를 호출했으나, 현재 프로젝트에는 empty() 정적 메서드가 없음.
        //        → null을 넘기면 of(...) 내부에서 대체값을 보장하므로 of(null) 사용.
        return b.actor(NotificationActorDto.of(null))
                .text(code != null ? code.name() : "NOTIFICATION")
                .build();
    }

    // ===================== [내부 유틸 - 주석 추가] =====================
    // - squashWhitespace: 줄바꿈/탭/연속 공백을 단일 스페이스로 축약하여 알림 카드에서 안정적으로 보이게 함
    private static String squashWhitespace(String s) {
        if (s == null) return "";
        // \s+ 는 줄바꿈/탭 등 포함 → 한 칸으로 축약
        String collapsed = s.replaceAll("\\s+", " ").trim();
        return collapsed;
    }

    // - ellipsize: 최대 길이를 초과하면 말줄임표(...) 처리
    //   (2줄 제한은 프런트 CSS로 처리하되, 비정상적으로 긴 한 줄을 방지하기 위한 1차 안전장치)
    private static String ellipsize(String s, int max) {
        if (s == null) return "";
        if (max <= 0) return "";
        if (s.length() <= max) return s;
        // 말줄임표는 "..." 고정 (OS 알림/웹 모두에서 일관성)
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
