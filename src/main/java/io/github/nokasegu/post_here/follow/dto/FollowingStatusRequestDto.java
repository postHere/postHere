package io.github.nokasegu.post_here.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** 배치 상태 요청 */
/** 화면에 표시 중인 사용자 id 목록을 서버로 보냄 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowingStatusRequestDto {

    private List<Long> ids;  // ← getIds()를 위해 @Getter 필수

    public static Object convertToEntity(FollowingStatusRequestDto d) { return null; }
    public static FollowingStatusRequestDto convertToDto(Object unused) { return null; }
}