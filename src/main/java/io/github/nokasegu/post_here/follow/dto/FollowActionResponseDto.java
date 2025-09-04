package io.github.nokasegu.post_here.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팔로우/언팔 이후 상태 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowActionResponseDto {
    private Long userId;
    private boolean following; // true=팔로잉 중, false=언팔 상태
}
