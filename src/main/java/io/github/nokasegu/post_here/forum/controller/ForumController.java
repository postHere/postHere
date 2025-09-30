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
    public WrapperDTO<String> setForumArea(@RequestBody ForumAreaRequestDto requestDto) {
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
