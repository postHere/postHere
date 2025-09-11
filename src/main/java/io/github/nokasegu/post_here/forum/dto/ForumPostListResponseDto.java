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

    private Long id;
    private String location;
    private String contentsText;
    private String writerNickname;
    private List<String> imageUrls;
    private String musicApiUrl;
    private String writerProfilePhotoUrl;
    private int totalComments;

    private LocalDateTime createdAt;

    public ForumPostListResponseDto(
            ForumEntity forumEntity,
            int totalComments,
            LocalDateTime createdAt
    ) {
        this.id = forumEntity.getId();
        // ForumAreaEntity 객체에서 address 필드를 가져와 String으로 설정
        this.location = forumEntity.getLocation().getAddress();
        this.contentsText = forumEntity.getContentsText();
        this.writerNickname = forumEntity.getWriter().getNickname();
        this.imageUrls = forumEntity.getImages().stream()
                .map(ForumImageEntity::getImgUrl)
                .collect(Collectors.toList());
        this.musicApiUrl = forumEntity.getMusicApiUrl();
        this.writerProfilePhotoUrl = forumEntity.getWriter().getProfilePhotoUrl();
        this.totalComments = totalComments;
        this.createdAt = forumEntity.getCreatedAt();
    }
}
