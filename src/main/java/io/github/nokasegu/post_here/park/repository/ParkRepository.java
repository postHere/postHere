package io.github.nokasegu.post_here.park.repository;

import io.github.nokasegu.post_here.park.domain.ParkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkRepository extends JpaRepository<ParkEntity, Long> {
}
