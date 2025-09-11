package io.github.nokasegu.post_here.notification.repository;

import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * [알림 생성 + 푸시 발사 사용 지점]
 * - WebPushService.sendToUser(...)에서 대상 유저의 구독 목록을 조회할 때 사용합니다.
 * - findAllByUser(...) 결과를 순회하며 각 endpoint로 푸시를 전송하는 구조입니다.
 */

/**
 * PushSubscriptionRepository
 * <p>
 * - 웹 푸시 알림(Web Push API)에서 사용자의 구독 정보를 DB에 저장/조회하기 위한 Repository
 * - 구독 정보(PushSubscriptionEntity)는 브라우저별 endpoint, 키 값 등을 포함
 * <p>
 * 작동 원리:
 * 1. 사용자가 알림을 허용하면 브라우저가 push 구독 정보를 생성
 * 2. 서버가 해당 구독 정보를 PushSubscriptionEntity로 저장
 * 3. 알림 발송 시 UserInfoEntity와 연결된 모든 구독 정보를 조회
 * 4. 각 endpoint로 푸시 서버에 메시지를 전달 → 브라우저에서 알림 표시
 * <p>
 * 메서드 설명:
 * - findAllByUser(UserInfoEntity user)
 * → 특정 유저의 모든 구독 정보를 반환 (디바이스/브라우저별 복수 지원)
 * <p>
 * - findByEndpoint(String endpoint)
 * → 이미 존재하는 구독인지 확인 (중복 방지용)
 * <p>
 * 확장 포인트:
 * - 향후 댓글, 좋아요, 친구 요청, 시스템 알림 등 다양한 알림 유형에서도 동일 구독 정보를 활용
 * - 필요 시 구독 상태(활성/비활성), 기기 타입, 알림 환경설정 필드 등을 추가 가능
 */
public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    // 특정 사용자(UserInfoEntity)에 연결된 모든 푸시 구독 정보 조회
    List<PushSubscriptionEntity> findAllByUser(UserInfoEntity user);

    // endpoint 값으로 구독 정보 단건 조회 (중복 등록 방지)
    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);
}
