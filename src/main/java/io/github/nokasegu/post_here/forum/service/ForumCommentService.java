package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.ForumCommentRequestDto;
import io.github.nokasegu.post_here.forum.dto.ForumCommentResponseDto;
import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final UserInfoRepository userInfoRepository;
    private final ForumRepository forumRepository; // 게시글을 찾기 위해 필요

    /**
     * 특정 게시글의 모든 댓글을 조회합니다.
     *
     * @param forumId   포럼 게시글 ID
     * @param userEmail 현재 로그인한 사용자의 이메일 (null일 수 있음)
     */
    public List<ForumCommentResponseDto> getComments(Long forumId, String userEmail) {
        // 현재 로그인한 사용자의 ID를 가져옵니다. 로그인하지 않았다면 null
        Long currentUserId = null;
        if (userEmail != null) {
            currentUserId = userInfoRepository.findByEmail(userEmail)
                    .map(UserInfoEntity::getId)
                    .orElse(null);
        }

        final Long finalCurrentUserId = currentUserId; // 람다식에서 사용하기 위해 final 변수로 선언

        return forumCommentRepository.findAllByForumIdOrderByCreatedAtAsc(forumId)
                .stream()
                .map(comment -> {
                    // 댓글 작성자 ID와 현재 사용자 ID를 비교하여 author 값을 결정
                    boolean author = finalCurrentUserId != null && comment.getWriter().getId().equals(finalCurrentUserId);
                    return new ForumCommentResponseDto(comment, author);
                })
                .collect(Collectors.toList());
    }


    /**
     * 댓글을 생성합니다.
     */
    public ForumCommentResponseDto createComment(Long forumId, ForumCommentRequestDto request, String userEmail) {
        // 사용자 및 포럼 게시글 엔티티 조회
        UserInfoEntity writer = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ForumEntity forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("Forum post not found"));

        // 댓글 엔티티 생성
        ForumCommentEntity newComment = ForumCommentEntity.builder()
                .forum(forum)
                .writer(writer)
                .contentsText(request.getContent())
                .build();

        // 댓글 저장
        ForumCommentEntity savedComment = forumCommentRepository.save(newComment);

        return new ForumCommentResponseDto(savedComment, true);
    }

    /**
     * 댓글 삭제
     *
     * @param commentId 삭제할 댓글의 ID
     * @param userEmail 현재 로그인한 사용자의 이메일
     */
    public void deleteComment(Long commentId, String userEmail) {
        // 1. 삭제할 댓글 엔티티 조회
        ForumCommentEntity comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // 2. 현재 로그인한 사용자 엔티티 조회
        UserInfoEntity currentUser = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 댓글 작성자 권한 확인
        // 댓글의 작성자 ID와 현재 사용자 ID를 비교하여, 동일한 경우에만 삭제를 허용
        if (!comment.getWriter().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("해당 댓글을 삭제할 권한이 없습니다.");
        }

        // 4. 권한 확인 후 댓글 삭제
        forumCommentRepository.delete(comment);
    }
}
