package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ForumCommentDto {
    private Long id;
    private String writerNickname;
    private String writerProfilePhotoUrl;
    private String contentsText;
    private LocalDateTime createdAt;
    private boolean isAuthor; // 내가 쓴 댓글인지 여부

    public ForumCommentDto(ForumCommentEntity entity, Long currentUserId) {
        this.id = entity.getId();
        this.writerNickname = entity.getWriter().getNickname();
        this.writerProfilePhotoUrl = entity.getWriter().getProfilePhotoUrl();
        this.contentsText = entity.getContentsText();
        this.createdAt = entity.getCreatedAt();

        if (currentUserId != null) {
            this.isAuthor = entity.getWriter().getId().equals(currentUserId);
        } else {
            this.isAuthor = false;
        }
    }
}
