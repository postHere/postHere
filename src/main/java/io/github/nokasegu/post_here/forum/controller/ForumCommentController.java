package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.forum.dto.ForumCommentRequestDto;
import io.github.nokasegu.post_here.forum.dto.ForumCommentResponseDto;
import io.github.nokasegu.post_here.forum.service.ForumCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/forum/{forumId}/comments")
public class ForumCommentController {

    private final ForumCommentService forumCommentService;

    /**
     * 특정 포럼 게시글의 댓글 목록을 조회하는 API
     * GET /forum/{forumId}/comments
     */
    @GetMapping
    public ResponseEntity<List<ForumCommentResponseDto>> getCommentsByForumId(@PathVariable Long forumId) {
        List<ForumCommentResponseDto> comments = forumCommentService.getComments(forumId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 포럼 게시글에 댓글을 작성하는 API
     * POST /forum/{forumId}/comments
     */
    @PostMapping
    public ResponseEntity<ForumCommentResponseDto> createComment(
            @PathVariable Long forumId,
            @RequestBody ForumCommentRequestDto request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail = principal.getName();

        ForumCommentResponseDto newComment = forumCommentService.createComment(forumId, request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }
}