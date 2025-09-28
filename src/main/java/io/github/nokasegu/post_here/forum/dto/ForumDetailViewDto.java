package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ForumDetailViewDto {

    private Long id;
    private String writerNickname;
    private String writerProfilePhotoUrl;
    private List<String> imageUrls;
    private String contentsText;
    private LocalDateTime createdAt;
    private boolean isAuthor;
    private int totalLikes;
    private boolean isLiked;
    private List<ForumCommentDto> comments;

    public ForumDetailViewDto(ForumEntity entity, int totalLikes, boolean isLiked, List<ForumCommentDto> comments, Long currentUserId) {
        this.id = entity.getId();
        this.contentsText = entity.getContentsText();
        this.createdAt = entity.getCreatedAt();
        this.totalLikes = totalLikes;
        this.isLiked = isLiked;
        this.comments = comments;

        // writer가 null일 경우를 대비한 안전장치 추가
        if (entity.getWriter() != null) {
            this.writerNickname = entity.getWriter().getNickname();
            this.writerProfilePhotoUrl = entity.getWriter().getProfilePhotoUrl();
            if (currentUserId != null) {
                this.isAuthor = entity.getWriter().getId().equals(currentUserId);
            } else {
                this.isAuthor = false;
            }
        } else {
            this.writerNickname = "알 수 없는 사용자";
            this.writerProfilePhotoUrl = "";
            this.isAuthor = false;
        }

        // images가 null일 경우를 대비한 안전장치 추가
        if (entity.getImages() != null) {
            this.imageUrls = entity.getImages().stream()
                    .map(image -> image.getImgUrl())
                    .collect(Collectors.toList());
        } else {
            this.imageUrls = Collections.emptyList();
        }
    }
}

