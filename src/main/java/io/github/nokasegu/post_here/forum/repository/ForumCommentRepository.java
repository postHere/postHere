// src/main/java/io/github/nokasegu/post_here/forum/repository/ForumCommentRepository.java
package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumCommentEntity, Long> {

    /**
     * 특정 포럼 게시글에 달린 모든 댓글을 작성 시간 순으로 조회합니다.
     *
     * @param forumId 포럼 게시글의 ID
     * @return 댓글 엔티티 리스트
     */
    List<ForumCommentEntity> findAllByForumIdOrderByCreatedAtAsc(Long forumId);

    // 특정 게시글의 댓글 총 개수를 조회하는 메서드 추가
    int countByForumId(Long forumId);

    // ===================== [신규] REQUIRES_NEW 안전용 fetch-join =====================
    // - REQUIRES_NEW 트랜잭션에서 넘겨받은 detached 엔티티의 지연로딩 접근으로 인한
    //   LazyInitializationException을 피하기 위해, 필요한 연관을 fetch-join으로 미리 로딩한다.
    @Query("""
            select c
              from ForumCommentEntity c
              join fetch c.writer cw
              join fetch c.forum f
              join fetch f.writer fw
             where c.id = :id
            """)
    Optional<ForumCommentEntity> findByIdWithWriterAndForumWriter(@Param("id") Long id);
}
