package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.dto.*;
import io.github.nokasegu.post_here.forum.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/start")
    public String start() {
        return "redirect:/forumMain";
    }

    @GetMapping("/forumMain")
    public String forumPage(Model model) {
        return "forum/main";
    }

    @GetMapping("/forum")
    public String forumWritePage() {
        return "forum/forum-write";
    }

    @ResponseBody
    @PostMapping(value = "/forum")
    public WrapperDTO<ForumCreateResponseDto> createForum(
            @RequestBody ForumCreateRequestDto requestDto,
            Principal principal) throws IOException {
        String userEmail = principal.getName();
        requestDto.setUserEmail(userEmail);

        // 이미지 ID 목록을 담은 DTO를 서비스로 전달
        ForumCreateResponseDto responseData = forumService.createForum(requestDto);

        return WrapperDTO.<ForumCreateResponseDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(responseData)
                .build();
    }

    /**
     * 게시글 수정 페이지로 이동
     *
     * @param forumId     수정할 게시글 ID
     * @param userDetails 현재 사용자 정보 (권한 확인용)
     * @param model       Thymeleaf 모델
     * @return 수정 페이지 뷰
     */
    @GetMapping("/forum/{forumId}/edit")
    public String editForumPage(
            @PathVariable("forumId") Long forumId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        // 현재 사용자 ID를 서비스에 전달하여 게시글 정보와 권한을 함께 확인합니다.
        ForumDetailResponseDto forumDetail = forumService.getForumDetail(forumId, userDetails.getUserInfo().getId());

        // 모델에 게시글 정보를 추가
        model.addAttribute("forum", forumDetail);

        return "forum/forum-edit";
    }


    /**
     * 게시글 수정 API
     *
     * @param forumId     수정할 게시글 ID
     * @param requestDto  수정할 데이터와 삭제할 이미지 ID 목록
     * @param userDetails 현재 사용자 정보
     * @return 성공 메시지
     */
    @ResponseBody
    @PostMapping("/forum/{forumId}")
    public WrapperDTO<String> updateForum(
            @PathVariable("forumId") Long forumId,
            @RequestBody ForumUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 클라이언트에서 보낸 DTO를 서비스로 전달
        forumService.updateForum(forumId, requestDto, userDetails.getUserInfo().getId());
        return WrapperDTO.<String>builder()
                .status(Code.OK.getCode())
                .message("게시글이 성공적으로 수정되었습니다.")
                .data("/forumMain")
                .build();
    }

    /**
     * 게시글 삭제 API
     *
     * @param forumId     삭제할 게시글 ID
     * @param userDetails 현재 사용자 정보
     * @return 성공 메시지
     */
    @ResponseBody
    @DeleteMapping("/forum/{forumId}")
    public WrapperDTO<String> deleteForum(
            @PathVariable("forumId") Long forumId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        forumService.deleteForum(forumId, userDetails.getUserInfo().getId());
        return WrapperDTO.<String>builder()
                .status(Code.OK.getCode())
                .message("게시글이 성공적으로 삭제되었습니다.")
                .build();
    }

    // 포럼 목록 열람 API
    @ResponseBody
    @GetMapping("/forum/area/{key}")
    public WrapperDTO<List<ForumPostListResponseDto>> getForumPostsByLocation(
            @PathVariable("key") String locationKey,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // UserDetails의 userId를 직접 가져옴
        Long currentUserId = userDetails != null ? userDetails.getUserInfo().getId() : null;

        // 서비스 메서드에 currentUserId를 명시적으로 전달합니다.
        List<ForumPostListResponseDto> forumPosts = forumService.getForumPostsByLocation(locationKey, currentUserId);

        return WrapperDTO.<List<ForumPostListResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(forumPosts)
                .build();
    }

    // 지역 검색 페이지로 이동
    @GetMapping("/forum/area")
    public String showForumAreaSearchPage() {
        return "forum/forum-area-search";
    }

    // 선택된 지역을 세션에 저장하고, 리다이렉트 URL을 JSON으로 반환합니다.
    @ResponseBody
    @PostMapping("/forum/searchArea")
    public WrapperDTO<String> setForumArea(
            @RequestBody ForumAreaRequestDto requestDto) {
        ForumAreaEntity area = forumService.getAreaByAddress(requestDto.getLocation());
        String redirectUrl = "/forumMain?areaKey=" + area.getId() + "&areaName=" + area.getAddress();
        return WrapperDTO.<String>builder()
                .status(Code.OK.getCode())
                .message("지역 설정이 성공적으로 변경되었습니다.")
                .data(redirectUrl)
                .build();
    }

    // 모든 지역 목록을 조회하는 API (검색 기능에 필요합니다)
    @ResponseBody
    @GetMapping("/forum/areas")
    public WrapperDTO<List<ForumAreaResponseDto>> getAllAreas() {
        List<ForumAreaResponseDto> areas = forumService.getAllAreas();
        return WrapperDTO.<List<ForumAreaResponseDto>>builder()
                .status(Code.OK.getCode())
                .message("지역 목록을 성공적으로 불러왔습니다.")
                .data(areas)
                .build();
    }

    /**
     * [추가] 현재 로그인된 사용자의 Forum 게시물 목록을 반환하는 API
     */
    // 미사용으로 추정
    @ResponseBody
    @GetMapping("/forums/my-posts")
    public ResponseEntity<Page<ForumPostSummaryDto>> getMyForums(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 4) Pageable pageable) {

        if (userDetails == null) return ResponseEntity.status(401).build();

        Page<ForumPostSummaryDto> result = forumService.getMyForums(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("/profile/forumlist/{nickname}")
    public ResponseEntity<Page<ForumPostSummaryDto>> getForumsForUser(
            @PathVariable String nickname,
            @PageableDefault(size = 4) Pageable pageable) {

        Page<ForumPostSummaryDto> result = forumService.getForumsByNickname(nickname, pageable);
        return ResponseEntity.ok(result);
    }
}
