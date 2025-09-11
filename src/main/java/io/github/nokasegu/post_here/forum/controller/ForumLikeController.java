package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.forum.dto.ForumLikeResponseDto;
import io.github.nokasegu.post_here.forum.service.ForumLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class ForumLikeController {

    private final ForumLikeService forumLikeService;

    /**
     * 좋아요를 누르거나 취소하는 API
     *
     * @param forumId 좋아요를 토글할 게시글 ID
     * @param liker   현재 인증된 사용자 정보 (CustomUserDetails)
     * @return 변경된 좋아요 상태를 포함한 응답
     */
    @ResponseBody
    @PostMapping("/forum/like/{forumId}")
    public WrapperDTO<ForumLikeResponseDto> toggleLike(
            @PathVariable("forumId") Long forumId,
            // ✅ @AuthenticationPrincipal을 CustomUserDetails로 직접 받습니다.
            @AuthenticationPrincipal CustomUserDetails liker) {

        // ✅ CustomUserDetails 객체에서 사용자 ID를 직접 가져옵니다.
        Long likerId = liker.getUserInfo().getId();

        // ForumLikeService에 좋아요 토글 로직 위임
        ForumLikeResponseDto responseDto = forumLikeService.toggleLike(forumId, likerId);

        return WrapperDTO.<ForumLikeResponseDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(responseDto)
                .build();
    }
}
