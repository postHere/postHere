// src/main/java/io/github/nokasegu/post_here/forum/service/ForumCommentService.java
package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.ForumCommentRequestDto;
import io.github.nokasegu.post_here.forum.dto.ForumCommentResponseDto;
import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.notification.service.NotificationService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j // [추가] 로깅을 위해
public class ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final UserInfoRepository userInfoRepository;
    private final ForumRepository forumRepository; // 게시글을 찾기 위해 필요

    // =================== [중요 변경] 이벤트 퍼블리셔 제거 → 서비스 직접 호출 ===================
    // 기존: ApplicationEventPublisher + CommentCreatedEvent
    // 변경: 트랜잭션 "커밋 이후(afterCommit)"에 NotificationService 호출
    private final NotificationService notificationService;
    // =====================================================================================

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

        // =================== [중요 변경] 알림은 "커밋 이후"에 실행 ===================
        // - 동일 트랜잭션 내부에서 REQUIRES_NEW로 재조회하면 미커밋이라 조회 실패 가능 → afterCommit 사용
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // [수정] ID가 아닌 엔티티 자체를 전달 (NotificationService 시그니처와 일치)
                        notificationService.createCommentAndPush(savedComment); // REQUIRES_NEW + 안전 재조회
                    } catch (Exception e) {
                        log.warn("[NOTI] afterCommit send failed commentId={} err={}",
                                savedComment.getId(), e.toString(), e);
                    }
                }
            });
        } else {
            // 트랜잭션 동기화가 없는 경우(드물게) 즉시 시도
            try {
                // [수정] ID가 아닌 엔티티 자체를 전달
                notificationService.createCommentAndPush(savedComment);
            } catch (Exception e) {
                log.warn("[NOTI] immediate send failed commentId={} err={}",
                        savedComment.getId(), e.toString(), e);
            }
        }
        // ========================================================================

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
        if (!comment.getWriter().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("해당 댓글을 삭제할 권한이 없습니다.");
        }

        // 4. 권한 확인 후 댓글 삭제
        forumCommentRepository.delete(comment);
    }
}
