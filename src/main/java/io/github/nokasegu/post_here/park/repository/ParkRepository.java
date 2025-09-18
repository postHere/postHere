package io.github.nokasegu.post_here.park.repository;

import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkRepository extends JpaRepository<ParkEntity, Long> {
    List<ParkEntity> findByOwner(UserInfoEntity owner);

    // ✅ [추가] 단일 조회 메소드도 다시 추가 (조회 로직이 단순해지므로)
    Optional<ParkEntity> findOneByOwner(UserInfoEntity owner);
}
