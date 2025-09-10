package io.github.nokasegu.post_here.follow.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.notification.service.NotificationService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 친구(팔로우) 서비스
 * - 컨트롤러가 요구하는 시그니처(getFollowers/getFollowings/getFollowingStatus/getUsersByNickname)에 맞춤
 * - 기존 Repository 메서드(findFollowersByMeId / findFollowingsByMeId / findFollowedIdsByMeIn)를 그대로 사용
 */
/* 추가 설명)
 * - 이 서비스는 "팔로우 생성" 같은 도메인 행위를 중심으로 여러 컴포넌트(Repository, NotificationService)를 오케스트레이션합니다.
 * - 팔로우가 새로 만들어질 때 NotificationService를 호출하여
 *   1) Notification 테이블에 레코드 생성
 *   2) WebPushService를 통해 대상 사용자 브라우저로 푸시 전송
 * - @Transactional 로직 안에서 이루어지므로, DB 쓰기와 알림 발사 트리거까지 하나의 도메인 흐름으로 묶여 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FollowingService {

    // 팔로우/팔로워 관계를 저장·조회하는 리포지토리 (following 테이블 접근)
    private final FollowingRepository followingRepository;

    // 사용자(UserInfoEntity) 조회를 위한 리포지토리 (user_info 테이블 접근)
    private final UserInfoRepository userInfoRepository;

    private final NotificationService notificationService; // ✅ 팔로우 시 알림 생성+푸시 연동
    // (추가 설명) NotificationService는 createFollowAndPush(FollowingEntity) 메서드를 제공하며,
    //  - 내부에서 Notification 엔티티를 INSERT
    //  - WebPushService.sendToUser(...)를 호출해 브라우저 구독 대상에게 실제 푸시를 발사합니다.

    /**
     * 팔로우 (idempotent)
     */
    public boolean follow(Long followerUserId, Long followedUserId) {
        // [검증 단계] 파라미터 유효성 및 자기 자신 팔로우 방지
        if (followerUserId == null || followedUserId == null) {
            throw new IllegalArgumentException("파라미터 누락");
        }
        if (Objects.equals(followerUserId, followedUserId)) {
            throw new IllegalArgumentException("자기 자신 팔로우 불가");
        }

        // [조회 단계] 팔로우를 시도하는 사용자(Actor)와 대상 사용자(Target) 로드
        UserInfoEntity follower = userInfoRepository.findById(followerUserId)
                .orElseThrow(() -> new IllegalArgumentException("follower 사용자 없음: " + followerUserId));
        UserInfoEntity followed = userInfoRepository.findById(followedUserId)
                .orElseThrow(() -> new IllegalArgumentException("followed 사용자 없음: " + followedUserId));

        // [멱등 처리] 이미 존재하는 관계라면 새로 만들지 않고 true 반환
        //  - 같은 요청이 중복으로 들어와도 사이드이펙트를 최소화합니다.
        if (followingRepository.existsByFollowerAndFollowed(follower, followed)) {
            return true; // 이미 팔로우 중이면 성공으로 처리
        }

        // [저장 단계] 새로운 팔로우 관계 생성
        FollowingEntity entity = FollowingEntity.builder()
                .follower(follower)
                .followed(followed)
                .build();
        followingRepository.save(entity);

        // ✅ 알림 생성 + WebPush 전송
        // (연결 지점) 여기서 NotificationService.createFollowAndPush(entity)를 호출하면,
        //   - Notification 레코드가 생성되고 (목표: followed 사용자)
        //   - 해당 사용자의 PushSubscription 목록에 대해 WebPush가 전송됩니다.
        // (참고) 푸시 페이로드에는 actor(=follower)의 닉네임/프로필, 알림 텍스트, notificationId, 라우팅용 URL 등이 포함됩니다.
        notificationService.createFollowAndPush(entity);

        // (추가 팁) 대량 동시 요청에서 중복 삽입을 더 강하게 막으려면
        //  DB에 (follower_id, followed_id) 유니크 제약을 두고, 예외 발생 시 멱등 처리로 흡수하는 전략을 권장합니다.
        return true;
    }

    /**
     * 언팔로우 (없어도 예외 없이 처리)
     */
    public boolean unfollow(Long followerUserId, Long followedUserId) {
        // [검증 단계]
        if (followerUserId == null || followedUserId == null) {
            throw new IllegalArgumentException("파라미터 누락");
        }
        if (Objects.equals(followerUserId, followedUserId)) {
            return false;
        }

        // [조회 단계] 존재하지 않으면 예외 (입력 오류 방지)
        UserInfoEntity follower = userInfoRepository.findById(followerUserId)
                .orElseThrow(() -> new IllegalArgumentException("follower 사용자 없음: " + followerUserId));
        UserInfoEntity followed = userInfoRepository.findById(followedUserId)
                .orElseThrow(() -> new IllegalArgumentException("followed 사용자 없음: " + followedUserId));

        // [삭제 단계] 관계가 있으면 삭제, 없으면 조용히 통과 → 멱등 보장
        followingRepository.deleteByFollowerAndFollowed(follower, followed);
        // (확장 포인트) 언팔로우 알림이 필요하면 여기에서 NotificationService를 호출하는 훅을 추가할 수 있습니다.
        return true;
    }

    // ===========================
    // FriendApiController에서 호출하는 메서드들
    // ===========================

    /**
     * 팔로워 목록(나를 팔로우) → Page<UserInfoEntity>
     * 기존 쿼리 메서드: findFollowersByMeId(meId, pageable)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowers(Long meUserId, Pageable pageable) {
        // (연결 지점) FriendApiController.followerList(...) 에서 호출
        //  - 반환 Page<UserInfoEntity>는 컨트롤러에서 DTO로 매핑되어 응답됩니다.
        return followingRepository.findFollowersByMeId(meUserId, pageable);
    }

    /**
     * 팔로잉 목록(내가 팔로우) → Page<UserInfoEntity>
     * 기존 쿼리 메서드: findFollowingsByMeId(meId, pageable)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowings(Long meUserId, Pageable pageable) {
        // (연결 지점) FriendApiController.followingList(...) 에서 호출
        return followingRepository.findFollowingsByMeId(meUserId, pageable);
    }

    /**
     * 상태 배치 조회: ids 중에서 내가 팔로우 중인 ID를 true로 표시
     * 기존 쿼리 메서드: findFollowedIdsByMeIn(meId, targetIds)
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getFollowingStatus(Long meUserId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();

        // (핵심) targetIds 중 "내가 팔로우하는" ID만 뽑아와서 Set으로 빠르게 조회
        List<Long> followedIds = followingRepository.findFollowedIdsByMeIn(meUserId, ids);
        Set<Long> followedSet = new HashSet<>(followedIds);

        // (출력 포맷) 입력한 ids 순서를 유지하기 위해 LinkedHashMap 사용
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long id : ids) {
            result.put(id, followedSet.contains(id));
        }
        return result;
    }

    /**
     * 닉네임 부분검색(로그인 사용자 제외)
     * 기존 쿼리 메서드: findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(meId, keyword, pageable)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getUsersByNickname(Long meUserId, String keyword, Pageable pageable) {
        // (NPE 방지) 빈 문자열로 정규화
        if (keyword == null) keyword = "";
        // (연결 지점) FriendApiController.search(...) 에서 호출
        //  - 자신(me) 제외, 닉네임 LIKE 검색, 정렬: nickname asc, id asc
        return userInfoRepository.findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(meUserId, keyword, pageable);
    }
}
