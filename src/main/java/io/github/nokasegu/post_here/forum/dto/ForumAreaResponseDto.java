package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForumAreaResponseDto {
    private Long id;
    private String address;

    public ForumAreaResponseDto(ForumAreaEntity entity) {
        this.id = entity.getId();
        this.address = entity.getAddress();
    }
}
