package io.github.nokasegu.post_here.follow.repository;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowingRepository extends JpaRepository<FollowingEntity, Long> {
}
