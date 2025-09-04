package io.github.nokasegu.post_here.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * addfollowing/unfollowing 공통 요청 바디
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowTargetRequestDto {
    private Long userId;
}
