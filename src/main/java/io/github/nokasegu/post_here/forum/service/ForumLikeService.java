package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.domain.ForumLikeEntity;
import io.github.nokasegu.post_here.forum.dto.ForumLikeResponseDto;
import io.github.nokasegu.post_here.forum.repository.ForumLikeRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumLikeService {

    // 좋아요 데이터 접근을 위한 리포지토리
    private final ForumLikeRepository forumLikeRepository;
    // 포럼 게시글 정보 접근을 위한 리포지토리
    private final ForumRepository forumRepository;
    // 사용자 정보 접근을 위한 리포지토리
    private final UserInfoRepository userInfoRepository;

    /**
     * 포럼 게시글의 좋아요 상태를 토글합니다.
     * 이미 좋아요를 눌렀다면 좋아요를 취소하고, 아니면 좋아요를 추가합니다.
     *
     * @param forumId 좋아요를 누를 게시글의 ID
     * @param likerId 좋아요를 누른 사용자의 ID
     * @return 변경된 좋아요 상태를 담은 DTO
     */
    public ForumLikeResponseDto toggleLike(Long forumId, Long likerId) {
        // 게시글 존재 여부 확인
        ForumEntity forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다."));

        // 사용자 존재 여부 확인
        UserInfoEntity liker = userInfoRepository.findById(likerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        // 기존에 좋아요를 눌렀는지 확인
        Optional<ForumLikeEntity> existingLike = forumLikeRepository.findByForumIdAndLikerId(forumId, likerId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // 좋아요가 존재하면 삭제
            forumLikeRepository.delete(existingLike.get());
            isLiked = false;
        } else {
            // 좋아요가 없으면 새로 생성하여 저장
            ForumLikeEntity newLike = ForumLikeEntity.builder()
                    .forum(forum)
                    .liker(liker)
                    .createdAt(LocalDateTime.now())
                    .build();
            forumLikeRepository.save(newLike);
            isLiked = true;
        }

        // DB에 다시 접근하지 않고, 변경된 상태를 기반으로 DTO를 만듭니다.
        // 좋아요 총 개수는 DB에서 다시 조회 (가장 정확한 최신 상태)
        int totalLikes = forumLikeRepository.countByForumId(forumId);
        List<String> recentLikerPhotos = forumLikeRepository.findTop3ByForumIdOrderByCreatedAtDesc(forumId)
                .stream()
                .map(like -> like.getLiker().getProfilePhotoUrl())
                .collect(Collectors.toList());

        return new ForumLikeResponseDto(totalLikes, recentLikerPhotos, isLiked);
    }

    /**
     * 특정 게시글의 좋아요 상태(총 개수, 최근 3명의 프로필 사진)를 조회합니다.
     * 이 메서드는 좋아요 여부(isLiked)를 판단하지 않고,
     * 게시글 자체의 좋아요 현황만 제공합니다.
     *
     * @param forumId 좋아요 상태를 조회할 게시글의 ID
     * @return 좋아요 상태를 담은 DTO
     */
    public ForumLikeResponseDto getLikeStatus(Long forumId) {
        // 특정 게시글의 좋아요 총 개수를 조회
        int totalLikes = forumLikeRepository.countByForumId(forumId);

        // 최근 좋아요를 누른 3명의 프로필 사진 URL 목록을 조회
        List<String> recentLikerPhotos = forumLikeRepository.findTop3ByForumIdOrderByCreatedAtDesc(forumId)
                .stream()
                .map(like -> like.getLiker().getProfilePhotoUrl())
                .collect(Collectors.toList());

        // 좋아요 여부는 이 메서드의 역할이 아니므로 false로 고정하여 반환
        return new ForumLikeResponseDto(totalLikes, recentLikerPhotos, false);
    }

}
