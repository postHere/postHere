package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumRepository extends JpaRepository<ForumEntity, Long> {

    // ForumAreaEntity 객체를 받아 해당 지역의 포럼 게시물을 모두 조회하는 메서드
    List<ForumEntity> findByLocation(ForumAreaEntity location);
}