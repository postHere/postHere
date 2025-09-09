package io.github.nokasegu.post_here.forum.repository;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForumAreaRepository extends JpaRepository<ForumAreaEntity, Long> {

    // 지역 주소(address)를 기준으로 ForumAreaEntity를 조회하는 메서드를 추가합니다.
    Optional<ForumAreaEntity> findByAddress(String address);
}
