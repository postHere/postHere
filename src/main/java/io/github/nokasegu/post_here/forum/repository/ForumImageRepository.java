package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.domain.ForumImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumImageRepository extends JpaRepository<ForumImageEntity, Long> {
    // 특정 ForumEntity에 속한 모든 ForumImageEntity를 찾기
    List<ForumImageEntity> findByForum(ForumEntity forum);

    // 특정 Forum ID에 속한 모든 이미지를 찾는 메소드 추가
    List<ForumImageEntity> findByForumId(Long forumId);
}
