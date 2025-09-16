package io.github.nokasegu.post_here.notification.repository;

import io.github.nokasegu.post_here.notification.domain.FcmTokenEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmTokenEntity, Long> {
    Optional<FcmTokenEntity> findByToken(String token);

    List<FcmTokenEntity> findAllByUser(UserInfoEntity user);
}
