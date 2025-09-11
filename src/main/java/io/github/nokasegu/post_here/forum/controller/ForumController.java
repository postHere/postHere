package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.forum.dto.*;
import io.github.nokasegu.post_here.forum.service.ForumService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/")
    public String baseRequest() {
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

        // 인증된 사용자의 이메일을 컨트롤러에서 직접 가져와 DTO에 설정
        String userEmail = principal.getName();
        requestDto.setUserEmail(userEmail);

        // DTO를 서비스로 전달
        ForumCreateResponseDto responseData = forumService.createForum(requestDto);

        return WrapperDTO.<ForumCreateResponseDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(responseData)
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
            @RequestBody ForumAreaRequestDto requestDto,
            HttpSession session) {
        Long areaKey = forumService.setForumArea(requestDto, session);
        String redirectUrl = "/forumMain?areaKey=" + areaKey;
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

    // 현재 위치 정보를 받아 해당 지역의 PK를 반환하는 API
    @ResponseBody
    @PostMapping("/location")
    public WrapperDTO<Long> updateCurrentLocation(@RequestBody ForumAreaRequestDto requestDto) {
        Long areaKey = forumService.getAreaKeyByAddress(requestDto.getLocation());
        return WrapperDTO.<Long>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(areaKey)
                .build();
    }
}
