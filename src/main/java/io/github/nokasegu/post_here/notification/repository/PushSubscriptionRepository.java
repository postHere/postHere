package io.github.nokasegu.post_here.notification.repository;

import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {
    List<PushSubscriptionEntity> findAllByUser(UserInfoEntity user);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);
}
