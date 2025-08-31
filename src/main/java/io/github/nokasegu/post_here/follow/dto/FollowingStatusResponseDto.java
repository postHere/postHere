package io.github.nokasegu.post_here.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 배치 상태 응답 */
/** key: 사용자 id, value: 내가 팔로우 중인지 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowingStatusResponseDto {

    private Map<Long, Boolean> status;

    public static FollowingStatusResponseDto from(Map<Long, Boolean> status) {
        return FollowingStatusResponseDto.builder().status(status).build();
    }

    public static Object convertToEntity(FollowingStatusResponseDto d) { return null; }
    public static FollowingStatusResponseDto convertToDto(Object unused) { return null; }
}

