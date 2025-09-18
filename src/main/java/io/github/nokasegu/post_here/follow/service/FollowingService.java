package io.github.nokasegu.post_here.follow.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FollowingService {

    private final FollowingRepository followingRepository;
    private final UserInfoRepository userInfoRepository;

    // ✅ 알림은 직접 호출하지 않고, "커밋 후" 처리되도록 이벤트만 발행
    private final ApplicationEventPublisher publisher;

    /**
     * 팔로우 (멱등)
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
            return true; // 이미 팔로우 중이면 성공 처리(멱등)
        }

        FollowingEntity entity = FollowingEntity.builder()
                .follower(follower)
                .followed(followed)
                .build();

        followingRepository.save(entity);
        followingRepository.flush(); // ⬅ INSERT 즉시 실행(제약 위반 등 조기 발견)

        // ✅ 트랜잭션 커밋 후(AFTER_COMMIT) 알림이 나가도록 이벤트만 발행
        publisher.publishEvent(new FollowCreatedEvent(follower.getId(), followed.getId(), entity.getId()));
        return true;
    }

    /**
     * 언팔로우 (멱등)
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

    /**
     * 팔로워 목록(나를 팔로우)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowers(Long meUserId, Pageable pageable) {
        return followingRepository.findFollowersByMeId(meUserId, pageable);
    }

    /**
     * 팔로잉 목록(내가 팔로우)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowings(Long meUserId, Pageable pageable) {
        return followingRepository.findFollowingsByMeId(meUserId, pageable);
    }

    /**
     * 상태 배치 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getFollowingStatus(Long meUserId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();

        List<Long> followedIds = followingRepository.findFollowedIdsByMeIn(meUserId, ids);
        Set<Long> set = new HashSet<>(followedIds);

        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long id : ids) result.put(id, set.contains(id));
        return result;
    }

    /**
     * 닉네임 검색(자기 자신 제외)
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getUsersByNickname(Long meUserId, String keyword, Pageable pageable) {
        if (keyword == null) keyword = "";
        return userInfoRepository.findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(meUserId, keyword, pageable);
    }

    // =======================
    // ✅ 커밋 후 알림용 이벤트 타입
    // =======================
    public static record FollowCreatedEvent(Long followerId, Long followedId, Long followingId) {
    }
}
