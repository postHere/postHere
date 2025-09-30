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
    public String forumPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUserInfo() != null) {
            var u = userDetails.getUserInfo();
            java.util.Map<String, Object> me = new java.util.HashMap<>();
            me.put("id", u.getId());
            me.put("nickname", u.getNickname());
            me.put("profilePhotoUrl", u.getProfilePhotoUrl());
            model.addAttribute("me", me);
        } else {
            model.addAttribute("me", null);
        }
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

        ForumCreateResponseDto responseData = forumService.createForum(requestDto);

        return WrapperDTO.<ForumCreateResponseDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(responseData)
                .build();
    }

    @GetMapping("/forum/{forumId}/edit")
    public String editForumPage(
            @PathVariable("forumId") Long forumId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        ForumDetailResponseDto forumDetail = forumService.getForumDetail(forumId, userDetails.getUserInfo().getId());
        model.addAttribute("forum", forumDetail);
        return "forum/forum-edit";
    }

    @ResponseBody
    @PostMapping("/forum/{forumId}")
    public WrapperDTO<String> updateForum(
            @PathVariable("forumId") Long forumId,
            @RequestBody ForumUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        forumService.updateForum(forumId, requestDto, userDetails.getUserInfo().getId());
        return WrapperDTO.<String>builder()
                .status(Code.OK.getCode())
                .message("게시글이 성공적으로 수정되었습니다.")
                .data("/forumMain")
                .build();
    }

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

    @ResponseBody
    @GetMapping("/forum/area/{key}")
    public WrapperDTO<List<ForumPostListResponseDto>> getForumPostsByLocation(
            @PathVariable("key") String locationKey,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails != null ? userDetails.getUserInfo().getId() : null;
        List<ForumPostListResponseDto> forumPosts = forumService.getForumPostsByLocation(locationKey, currentUserId);

        return WrapperDTO.<List<ForumPostListResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(forumPosts)
                .build();
    }

    @GetMapping("/forum/area")
    public String showForumAreaSearchPage() {
        return "forum/forum-area-search";
    }

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

    /**
     * Forum 피드 페이지
     */
    @GetMapping("/forum/feed")
    public String getForumFeedPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserInfo().getId() : null;

        List<ForumPostListResponseDto> posts = forumService.getAllForumPostsForFeed(currentUserId);
        model.addAttribute("posts", posts);

        // 🔹 우측 슬라이드 댓글 모달의 에디터 아바타용 로그인 사용자 주입
        if (userDetails != null && userDetails.getUserInfo() != null) {
            var u = userDetails.getUserInfo();
            java.util.Map<String, Object> me = new java.util.HashMap<>();
            me.put("id", u.getId());
            me.put("nickname", u.getNickname());
            me.put("profilePhotoUrl", u.getProfilePhotoUrl());
            model.addAttribute("me", me);
        } else {
            model.addAttribute("me", null);
        }

        return "forum/feed";
    }
}
