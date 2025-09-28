package io.github.nokasegu.post_here.forum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ForumPostSummaryDto {
    private Long id;
    private String imageUrl;
    private String location;
    private String contentsText;
}