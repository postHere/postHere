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
@Service
@RequiredArgsConstructor
@Transactional
public class FollowingService {

    private final FollowingRepository followingRepository;
    private final UserInfoRepository userInfoRepository;
    private final NotificationService notificationService; // ✅ 팔로우 시 알림 생성+푸시 연동

    /**
     * 팔로우 (idempotent)
     */
    public boolean follow(Long followerUserId, Long followedUserId) {
        if (followerUserId == null || followedUserId == null) {
            throw new IllegalArgumentException("파라미터 누락");
        }
        if (Objects.equals(followerUserId, followedUserId)) {
            throw new IllegalArgumentException("자기 자신 팔로우 불가");
        }

        UserInfoEntity follower = userInfoRepository.findById(followerUserId)
                .orElseThrow(() -> new IllegalArgumentException("follower 사용자 없음: " + followerUserId));
        UserInfoEntity followed = userInfoRepository.findById(followedUserId)
                .orElseThrow(() -> new IllegalArgumentException("followed 사용자 없음: " + followedUserId));

        if (followingRepository.existsByFollowerAndFollowed(follower, followed)) {
            return true; // 이미 팔로우 중이면 성공으로 처리
        }

        FollowingEntity entity = FollowingEntity.builder()
                .follower(follower)
                .followed(followed)
                .build();
        followingRepository.save(entity);

        // ✅ 알림 생성 + WebPush 전송
        notificationService.createFollowAndPush(entity);

        return true;
    }

    /**
     * 언팔로우 (없어도 예외 없이 처리)
     */
    public boolean unfollow(Long followerUserId, Long followedUserId) {
        if (followerUserId == null || followedUserId == null) {
            throw new IllegalArgumentException("파라미터 누락");
        }
        if (Objects.equals(followerUserId, followedUserId)) {
            return false;
        }

        UserInfoEntity follower = userInfoRepository.findById(followerUserId)
                .orElseThrow(() -> new IllegalArgumentException("follower 사용자 없음: " + followerUserId));
        UserInfoEntity followed = userInfoRepository.findById(followedUserId)
                .orElseThrow(() -> new IllegalArgumentException("followed 사용자 없음: " + followedUserId));

        followingRepository.deleteByFollowerAndFollowed(follower, followed);
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
        return followingRepository.findFollowersByMeId(meUserId, pageable);
    }

    /**
     * 팔로잉 목록(내가 팔로우) → Page<UserInfoEntity>
     * 기존 쿼리 메서드: findFollowingsByMeId(meId, pageable)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowings(Long meUserId, Pageable pageable) {
        return followingRepository.findFollowingsByMeId(meUserId, pageable);
    }

    /**
     * 상태 배치 조회: ids 중에서 내가 팔로우 중인 ID를 true로 표시
     * 기존 쿼리 메서드: findFollowedIdsByMeIn(meId, targetIds)
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getFollowingStatus(Long meUserId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();

        List<Long> followedIds = followingRepository.findFollowedIdsByMeIn(meUserId, ids);
        Set<Long> followedSet = new HashSet<>(followedIds);

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
        if (keyword == null) keyword = "";
        return userInfoRepository.findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(meUserId, keyword, pageable);
    }
}
