package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.forum.dto.ForumCreateRequestDto;
import io.github.nokasegu.post_here.forum.dto.ForumCreateResponseDto;
import io.github.nokasegu.post_here.forum.dto.ForumPostListResponseDto;
import io.github.nokasegu.post_here.forum.service.ForumService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
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
        // TODO: 실제로는 ForumService를 통해 DB에서 게시글 목록을 조회해야 합니다.
        // 지금은 UI를 보여주기 위한 더미 데이터를 생성합니다.
        List<ForumCreateRequestDto> posts = new ArrayList<>();
        posts.add(ForumCreateRequestDto.builder()
                .writerId(1L)
                .content("Post description goes here. It can be a question, a thought, or anything!")
                .location(10L)
                .build());
        posts.add(ForumCreateRequestDto.builder()
                .writerId(53L)
                .content("아무말")
                .location(8L)
                .build());

        model.addAttribute("posts", posts);
        return "forum/main";
    }

    @GetMapping("/forum")
    public String forumWritePage() {
        return "forum/forum-write";
    }

    @ResponseBody
    @PostMapping(value = "/forum")
    public WrapperDTO<ForumCreateResponseDto> createForum(
            @RequestBody ForumCreateRequestDto requestDto, // Changed from @ModelAttribute
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

    // 포럼 목록 열람 API
    @ResponseBody
    @GetMapping("/forum/area/{key}")
    public WrapperDTO<List<ForumPostListResponseDto>> getForumPostsByLocation(
            @PathVariable("key") String locationKey,
            HttpSession session) {

        // 세션에 저장된 지역 정보가 있다면 우선적으로 사용합니다.
        String selectedArea = (String) session.getAttribute("selectedForumAreaAddress");
        String finalLocation = selectedArea != null ? selectedArea : locationKey;

        List<ForumPostListResponseDto> forumPosts = forumService.getForumPostsByLocation(finalLocation);

        return WrapperDTO.<List<ForumPostListResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(forumPosts)
                .build();
    }
}
