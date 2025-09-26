package io.github.nokasegu.post_here.location.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LocationResponseDto {

    private String forumKey;
    private String forumName;
}
