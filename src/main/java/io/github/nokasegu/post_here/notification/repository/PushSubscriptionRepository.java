package io.github.nokasegu.post_here.notification.repository;

import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * [도메인 저장소 개요]
 * - Web Push 구독 정보를 영속 계층에서 관리하는 JPA Repository.
 * - WebPushService.sendToUser(...)가 사용자별 구독 목록을 조회하는 주요 진입점.
 */
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    // 특정 사용자(UserInfoEntity)에 연결된 모든 푸시 구독 정보 조회
    List<PushSubscriptionEntity> findAllByUser(UserInfoEntity user);

    // [변경] endpoint만으로 찾으면 중복 문제 발생 → user + endpoint 조합으로 조회
    Optional<PushSubscriptionEntity> findByUserAndEndpoint(UserInfoEntity user, String endpoint);
}
