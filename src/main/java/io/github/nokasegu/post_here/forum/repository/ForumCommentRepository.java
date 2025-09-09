package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumCommentEntity, Long> {

    /**
     * 특정 포럼 게시글에 달린 모든 댓글을 작성 시간 순으로 조회합니다.
     *
     * @param forumId 포럼 게시글의 ID
     * @return 댓글 엔티티 리스트
     */
    public List<ForumCommentEntity> findAllByForumIdOrderByCreatedAtAsc(Long forumId);

}
