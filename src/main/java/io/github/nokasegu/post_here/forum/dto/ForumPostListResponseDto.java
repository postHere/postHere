package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.domain.ForumImageEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ForumPostListResponseDto {

    private final Long id;
    private final String location;
    private final String contentsText;
    private final String writerNickname;
    private final Long writerId;
    private final List<String> imageUrls;
    private final String writerProfilePhotoUrl;
    private final int totalComments;
    private final boolean author;
    private final LocalDateTime createdAt;

    // 좋아요 관련 필드
    private final int totalLikes;
    private final boolean isLiked;
    private final List<String> recentLikerPhotos;

    public ForumPostListResponseDto(
            ForumEntity forumEntity,
            int totalComments,
            LocalDateTime createdAt,
            int totalLikes,
            boolean isLiked,
            List<String> recentLikerPhotos,
            boolean author
    ) {
        this.id = forumEntity.getId();
        // ForumAreaEntity 객체에서 address 필드를 가져와 String으로 설정
        this.location = forumEntity.getLocation().getAddress();
        this.contentsText = forumEntity.getContentsText();
        this.writerNickname = forumEntity.getWriter().getNickname();
        this.writerId = forumEntity.getWriter().getId();
        this.imageUrls = forumEntity.getImages().stream()
                .map(ForumImageEntity::getImgUrl)
                .collect(Collectors.toList());
        this.writerProfilePhotoUrl = forumEntity.getWriter().getProfilePhotoUrl();
        this.totalComments = totalComments;
        this.createdAt = forumEntity.getCreatedAt();
        this.totalLikes = totalLikes;
        this.isLiked = isLiked;
        this.recentLikerPhotos = recentLikerPhotos;
        this.author = author;
    }
}
