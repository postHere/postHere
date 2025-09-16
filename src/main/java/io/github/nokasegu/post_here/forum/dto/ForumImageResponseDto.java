package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumImageEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForumImageResponseDto {
    private Long id;
    private String imgUrl;

    public ForumImageResponseDto(ForumImageEntity entity) {
        this.id = entity.getId();
        this.imgUrl = entity.getImgUrl();
    }
}
