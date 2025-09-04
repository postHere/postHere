package io.github.nokasegu.post_here.forum.dto;

import lombok.Getter;

@Getter
public class ForumCreateResponseDto {

    private final Long forumId;

    public ForumCreateResponseDto(Long forumId) {
        this.forumId = forumId;
    }
}
