package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumLikeRepository extends JpaRepository<ForumLikeEntity, Long> {

    // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
    Optional<ForumLikeEntity> findByForumIdAndLikerId(Long forumId, Long likerId);

    // 특정 게시글의 좋아요 총 개수 조회
    int countByForumId(Long forumId);

    // 특정 게시글에 좋아요를 누른 최근 3명의 사용자 정보(프로필 사진 URL) 조회
    List<ForumLikeEntity> findTop3ByForumIdOrderByCreatedAtDesc(Long forumId);
    
}
