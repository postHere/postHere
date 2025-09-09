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
public class ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final UserInfoRepository userInfoRepository;
    private final ForumRepository forumRepository; // 게시글을 찾기 위해 필요

    /**
     * 특정 게시글의 모든 댓글을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ForumCommentResponseDto> getComments(Long forumId) {
        return forumCommentRepository.findAllByForumIdOrderByCreatedAtAsc(forumId)
                .stream()
                .map(ForumCommentResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 댓글을 생성합니다.
     */
    @Transactional
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

        return new ForumCommentResponseDto(savedComment);
    }
}
