package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumRepository extends JpaRepository<ForumEntity, Long> {

    // ForumAreaEntity 객체를 받아 해당 지역의 포럼 게시물을 모두 조회하는 메서드
    List<ForumEntity> findByLocation(ForumAreaEntity location);

    Page<ForumEntity> findByWriterOrderByIdDesc(UserInfoEntity writer, Pageable pageable);

    // 프로필 페이지의 이미지/위치 문제를 해결하기 위한 메소드
    @Query("SELECT f FROM ForumEntity f LEFT JOIN FETCH f.images LEFT JOIN FETCH f.location WHERE f.writer = :writer ORDER BY f.id DESC")
    Page<ForumEntity> findByWriterForProfile(@Param("writer") UserInfoEntity writer, Pageable pageable);

    @Query("SELECT f FROM ForumEntity f WHERE f.writer = :writer")
        // 피드 페이지에서 모든 게시물을 최신순으로 가져오기 위해 필요합니다.
    List<ForumEntity> findAllByOrderByCreatedAtDesc(@Param(":writer") UserInfoEntity writer);

    // 상세 보기(댓글 모달) 기능에 필요합니다.
    @Query("SELECT f FROM ForumEntity f LEFT JOIN FETCH f.writer LEFT JOIN FETCH f.images WHERE f.id = :id")
    Optional<ForumEntity> findByIdWithDetails(@Param("id") Long id);
}
