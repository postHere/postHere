package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.*;
import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import io.github.nokasegu.post_here.forum.repository.ForumLikeRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.forum.service.ForumService;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final ForumRepository forumRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final ForumLikeRepository forumLikeRepository;

    @GetMapping("/start")
    public String start() {
        return "redirect:/forumMain";
    }

    @GetMapping("/forumMain")
    public String forumPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 로그인 사용자 정보를 'me'에 주입 (댓글 모달 등에서 아바타 표시용)
        if (userDetails != null && userDetails.getUserInfo() != null) {
            var u = userDetails.getUserInfo();
            Map<String, Object> me = new HashMap<>();
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
    @PostMapping("/forum")
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

        ForumDetailResponseDto forumDetail =
                forumService.getForumDetail(forumId, userDetails.getUserInfo().getId());

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

    // ===== 상세 보기 (새 DTO 없이 Map으로 detail.html에 바인딩) =====
    @GetMapping("/forum/{forumId}")
    public String forumDetailPage(
            @PathVariable("forumId") Long forumId,
            @RequestParam(value = "open", required = false) String open,
            @RequestParam(value = "commentId", required = false) Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        Long currentUserId = (userDetails != null && userDetails.getUserInfo() != null)
                ? userDetails.getUserInfo().getId()
                : null;

        // 게시글 + 작성자 + 이미지 로딩
        ForumEntity forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시물을 찾을 수 없습니다. id=" + forumId));

        // 좋아요/작성자 여부
        int totalLikes = forumLikeRepository.countByForumId(forumId);
        boolean isLiked = false;
        boolean isAuthor = false;
        if (currentUserId != null && forum.getWriter() != null) {
            isLiked = forumLikeRepository.findByForumIdAndLikerId(forumId, currentUserId).isPresent();
            isAuthor = forum.getWriter().getId().equals(currentUserId);
        }

        // 댓글 리스트 (detail.html이 기대하는 필드명으로 매핑)
        var comments = forumCommentRepository.findAllByForumIdOrderByCreatedAtAsc(forumId)
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("writerNickname", c.getWriter() != null ? c.getWriter().getNickname() : "알 수 없음");
                    m.put("writerProfilePhotoUrl", c.getWriter() != null ? c.getWriter().getProfilePhotoUrl() : null);
                    m.put("contentsText", c.getContentsText());
                    return m;
                })
                .collect(Collectors.toList());

        // detail.html이 참조하는 'post' 구조 구축
        Map<String, Object> post = new HashMap<>();
        post.put("id", forum.getId());
        post.put("writerNickname", forum.getWriter() != null ? forum.getWriter().getNickname() : "알 수 없는 사용자");
        post.put("writerProfilePhotoUrl", forum.getWriter() != null ? forum.getWriter().getProfilePhotoUrl() : null);
        post.put("imageUrls", forum.getImages() != null
                ? forum.getImages().stream().map(img -> img.getImgUrl()).collect(Collectors.toList())
                : Collections.emptyList());
        post.put("contentsText", forum.getContentsText());
        post.put("createdAt", forum.getCreatedAt());
        post.put("isAuthor", isAuthor);
        post.put("totalLikes", totalLikes);
        post.put("isLiked", isLiked);
        post.put("comments", comments);

        model.addAttribute("post", post);
        model.addAttribute("openComments", "comments".equalsIgnoreCase(open));
        model.addAttribute("targetCommentId", commentId);

        return "forum/detail";
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
        List<ForumPostListResponseDto> forumPosts =
                forumService.getForumPostsByLocation(locationKey, currentUserId);

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
    public WrapperDTO<String> setForumArea(@RequestBody ForumAreaRequestDto requestDto) {
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
        Page<ForumPostSummaryDto> result =
                forumService.getMyForums(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("/profile/forumlist/{nickname}")
    public ResponseEntity<Page<ForumPostSummaryDto>> getForumsForUser(
            @PathVariable String nickname,
            @PageableDefault(size = 4) Pageable pageable) {

        Page<ForumPostSummaryDto> result =
                forumService.getForumsByNickname(nickname, pageable);
        return ResponseEntity.ok(result);
    }
}
