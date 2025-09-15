package io.github.nokasegu.post_here.forum.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ForumImageUploadResponseDto {
    private Long imageId;
    private String imageUrl;
}
