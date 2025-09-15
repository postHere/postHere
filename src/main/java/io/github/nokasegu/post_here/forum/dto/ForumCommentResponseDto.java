package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ForumCommentResponseDto {

    private final Long id;
    private final String content;
    private final String authorNickname;
    private final String authorProfileImageUrl;
    private final LocalDateTime createdAt;

    private final boolean author; // 현재 사용자가 작성자인지 여부 추가

    public ForumCommentResponseDto(ForumCommentEntity comment, boolean author) {
        this.id = comment.getId();
        this.content = comment.getContentsText();
        this.authorNickname = comment.getWriter().getNickname();
        this.authorProfileImageUrl = comment.getWriter().getProfilePhotoUrl() != null
                ? comment.getWriter().getProfilePhotoUrl()
                : "https://placehold.co/40x40/E2E8F0/4A5568?text=U"; // 기본 이미지
        this.createdAt = comment.getCreatedAt();
        this.author = author;
    }
}
