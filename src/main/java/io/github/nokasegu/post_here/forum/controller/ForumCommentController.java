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
    public ResponseEntity<List<ForumCommentResponseDto>> getCommentsByForumId(
            @PathVariable Long forumId,
            Principal principal) {

        // Principal 객체에서 사용자 이메일을 가져옵니다. 로그인하지 않았다면 null
        String userEmail = (principal != null) ? principal.getName() : null;

        // 서비스 메서드에 사용자 이메일을 전달
        List<ForumCommentResponseDto> comments = forumCommentService.getComments(forumId, userEmail);
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

    /**
     * 특정 포럼 게시글의 댓글을 삭제하는 API
     * DELETE /forum/{forumId}/comments/{commentId}
     *
     * @param forumId   삭제할 댓글이 속한 게시글의 ID
     * @param commentId 삭제할 댓글의 ID
     * @param principal 현재 로그인한 사용자 정보
     * @return 성공 여부 HTTP 상태 코드
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long forumId,
            @PathVariable Long commentId,
            Principal principal) {

        // 1. 사용자 인증 확인
        // principal 객체가 null인지 확인하여 로그인 여부를 검증
        if (principal == null) {
            // 로그인되어 있지 않으면 401 Unauthorized 상태 코드를 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = principal.getName();

        // 2. 서비스 계층으로 댓글 삭제 로직 위임
        forumCommentService.deleteComment(commentId, userEmail);

        // 3. 성공 응답 반환
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}