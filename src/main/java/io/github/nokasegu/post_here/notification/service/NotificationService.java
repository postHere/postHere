package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.notification.dto.NotificationItemResponseDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListResponseDto;
import io.github.nokasegu.post_here.notification.repository.NotificationRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 알림 도메인 서비스
 * <p>
 * 역할
 * - 알림(NotificationEntity) 저장/조회/읽음 처리의 도메인 오케스트레이션 담당
 * - 특정 이벤트(여기서는 "팔로우") 발생 시 DB에 알림 레코드를 생성하고, WebPush 전송을 트리거
 * <p>
 * 외부 협력자
 * - NotificationRepository: 알림 영속화/조회/읽음처리
 * - UserInfoRepository: 타겟 유저 존재 검증 및 엔티티 로드
 * - WebPushService: DB 저장 후, 해당 타겟 유저의 모든 브라우저 구독으로 Web Push 전송
 * <p>
 * 트랜잭션 전략
 * - 기본 @Transactional: 쓰기 메서드는 트랜잭션 경계 내에서 알림 생성 후 푸시 전송 호출
 * (WebPush 전송 실패가 저장을 롤백하진 않음. 전송 예외는 내부에서 잡거나 별도 로깅 처리)
 * - 조회/카운트는 @Transactional(readOnly = true)
 * <p>
 * 프런트/서비스워커 연계
 * - WebPush 페이로드(JSON)는 service-worker.js의 'push' 핸들러와 합의된 스키마를 사용:
 * {
 * "type": "FOLLOW",
 * "notificationId": <알림 PK>,
 * "actor": { "nickname": "...", "profilePhotoUrl": "..." },
 * "text": "Started following you"
 * }
 * - SW는 이 JSON을 토대로 OS 알림을 띄우고, 클릭 시 '/notification?focus=<id>'로 이동하도록 구성
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository; // 알림 CRUD/배치 읽음처리
    private final UserInfoRepository userInfoRepository;         // 타겟 유저 검증/조회
    private final WebPushService webPushService;                 // Web Push 전송(브라우저 구독 대상)

    /**
     * [팔로우 알림 생성 및 Web Push 전송 시퀀스]
     * - 트리거: FollowingService.follow(...) 성공 시 호출됨
     * - 1) Notification(FOLLOW) INSERT
     * - 2) service-worker와 합의된 payload 구성(JSON)
     * - 3) WebPushService.sendToUser(...)로 대상 유저의 모든 구독 endpoint에 푸시 전송
     * - 주의: 푸시 실패는 저장을 롤백하지 않음(로그로 확인)
     */

    /**
     * 팔로우 이벤트 알림 생성 + WebPush 전송
     * <p>
     * 생성 규칙
     * - 코드: FOLLOW
     * - following 연관 저장(누가 누구를 팔로우했는지)
     * - targetUser: 팔로우 "당한" 유저 (알림 수신자)
     * - checkStatus: false(미읽음)
     * <p>
     * 푸시 페이로드(서비스워커와 합의된 스키마)
     * - type: "FOLLOW"
     * - notificationId: 방금 저장된 알림의 PK (클라이언트 라우팅/하이라이트에 사용)
     * - actor: 수행자(팔로우를 건 유저)의 표시 정보(닉네임/프로필 이미지)
     * - text: 표시 문구(간단 안내)
     * <p>
     * 주의
     * - 전송 실패가 저장을 롤백하진 않음(웹 푸시는 best-effort). 서버 로그로 확인 가능.
     */
    @Transactional
    public NotificationEntity createFollowAndPush(FollowingEntity following) {
        NotificationEntity n = NotificationEntity.builder()
                .notificationCode(NotificationCode.FOLLOW)
                .following(following)
                .targetUser(following.getFollowed())
                .checkStatus(false)
                .build();
        NotificationEntity saved = notificationRepository.save(n);

        // service-worker.js의 'push' 핸들러가 이해하는 JSON 페이로드 구성
        Map<String, Object> payload = Map.of(
                "type", "FOLLOW", // 클라이언트에서 알림 타입 분기 시 사용
                "notificationId", saved.getId(), // 클릭 이동 시 포커스용
                "actor", Map.of(
                        "nickname", following.getFollower().getNickname(),
                        "profilePhotoUrl", following.getFollower().getProfilePhotoUrl()
                ),
                "text", "Started following you"
        );

        // 타겟 유저의 모든 구독 엔드포인트로 WebPush 전송(성공/실패는 내부에서 로깅)
        webPushService.sendToUser(following.getFollowed(), payload);

        return saved;
    }

    /**
     * /notification/list 응답 조합
     * - items: 페이지 리스트(알림 카드)
     * - unreadCount: 미읽음 개수(종 아이콘 배지/점 갱신에 사용)
     */
    /**
     * 알림 목록 조회(+ 미읽음 카운트 함께 반환)
     * <p>
     * 파라미터
     * - targetUserId: 알림 수신자(로그인 사용자)의 PK
     * - page/size: 페이지네이션 파라미터 (PageRequest.of(page, size))
     * <p>
     * 반환
     * - items: NotificationEntity → NotificationItemResponseDto 매핑 리스트
     * - unreadCount: 현재 미읽음 개수
     * <p>
     * 예외
     * - targetUserId가 존재하지 않으면 NoSuchElementException (컨트롤러 단에서 적절히 처리)
     */
    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        var list = notificationRepository.findListByTarget(target, PageRequest.of(page, size));
        long unread = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
        var items = list.stream().map(NotificationItemResponseDto::fromEntity).toList();
        return NotificationListResponseDto.of(items, unread);
    }

    /**
     * /notification/read (멱등)
     * - 클라이언트가 방금 본 알림 ID들을 넘기면 읽음으로 마킹
     * - 반환값: 남은 미읽음 개수(배지/점 갱신)
     */
    /**
     * 지정한 알림 ID 집합을 읽음 처리 (idempotent)
     * <p>
     * 동작
     * - ids가 비어있으면 아무것도 하지 않고 현재 미읽음 개수만 반환
     * - 타겟 유저의 소유 알림 중 ids에 포함된 항목만 checkStatus=true로 마킹
     * - 최종적으로 남은 미읽음 개수 반환 (UI 배지/점 갱신에 사용)
     */
    @Transactional
    public long markRead(Long targetUserId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return unreadCount(targetUserId);
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        notificationRepository.markReadByIds(target, ids);
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }

    /**
     * 전체 읽음 처리 훅
     * - 페이지/설정에서 "모두 읽음" 액션 시 사용 가능
     */
    /**
     * 해당 유저의 모든 알림을 읽음 처리
     * <p>
     * 동작
     * - targetUser의 모든 미읽음 알림을 checkStatus=true로 마킹
     * - 남은 미읽음 개수 반환(대개 0)
     */
    @Transactional
    public long markAllRead(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        notificationRepository.markAllRead(target);
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }

    /**
     * /notification/unread-count
     * - 하단 네비 종 아이콘 빨간점 표시/해제에 활용
     */
    /**
     * 미읽음 카운트 조회
     * <p>
     * 용도
     * - 하단 네비 종 아이콘 빨간 점(on/off) 표시
     * - Notification 페이지 진입 시 상단 배지 렌더링
     */
    @Transactional(readOnly = true)
    public long unreadCount(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId).orElseThrow();
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }
}
