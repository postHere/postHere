package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ForumDetailResponseDto {

    private Long id;
    private String contentsText;
    private List<ForumImageResponseDto> images;
    private Long locationId;
    private boolean isAuthor;

    public ForumDetailResponseDto(ForumEntity forumEntity, boolean isAuthor) {
        this.id = forumEntity.getId();
        this.contentsText = forumEntity.getContentsText();
        this.locationId = forumEntity.getLocation().getId();
        this.images = forumEntity.getImages().stream()
                .map(ForumImageResponseDto::new)
                .collect(Collectors.toList());
        this.isAuthor = isAuthor;
    }
}
