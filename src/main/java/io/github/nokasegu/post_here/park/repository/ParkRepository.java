package io.github.nokasegu.post_here.park.repository;

import io.github.nokasegu.post_here.park.domain.ParkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkRepository extends JpaRepository<ParkEntity, Long> {
    /**
     * 소유자(owner)의 ID를 기반으로 Park 정보를 조회합니다.
     *
     * @param ownerId Park 소유자의 User ID
     * @return Optional<ParkEntity>
     */
    public Optional<ParkEntity> findByOwnerId(Long ownerId);
}
