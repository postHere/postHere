package io.github.nokasegu.post_here.follow.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FollowingService {

    private final FollowingRepository followingRepository;
    private final UserInfoRepository userInfoRepository;

    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowers(Long meId, Pageable pageable) {
        return followingRepository.findFollowersByMeId(meId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getFollowings(Long meId, Pageable pageable) {
        return followingRepository.findFollowingsByMeId(meId, pageable);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getFollowingStatus(Long meId, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) return Map.of();
        List<Long> followedIds = followingRepository.findFollowedIdsByMeIn(meId, targetIds);
        Set<Long> set = new HashSet<>(followedIds);
        Map<Long, Boolean> res = new LinkedHashMap<>();
        for (Long id : targetIds) res.put(id, set.contains(id));
        return res;
    }

    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getUsersByNickname(Long meId, String keyword, Pageable pageable) {
        String k = (keyword == null) ? "" : keyword.trim();
        return userInfoRepository.findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(meId, k, pageable);
    }

    // ===== Follow
    @Transactional
    public boolean follow(Long meId, Long targetId) {
        if (meId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신은 팔로우할 수 없습니다.");
        }
        UserInfoEntity me = userInfoRepository.findById(meId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음: " + meId));
        UserInfoEntity target = userInfoRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 사용자 없음: " + targetId));

        if (followingRepository.existsByFollowerAndFollowed(me, target)) {
            return false; // 이미 팔로우 (idempotent)
        }
        FollowingEntity entity = FollowingEntity.builder()
                .follower(me)
                .followed(target)
                .build();
        followingRepository.save(entity);
        return true;
    }

    // ===== Unfollow
    @Transactional
    public boolean unfollow(Long meId, Long targetId) {
        UserInfoEntity me = userInfoRepository.findById(meId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음: " + meId));
        UserInfoEntity target = userInfoRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 사용자 없음: " + targetId));

        if (!followingRepository.existsByFollowerAndFollowed(me, target)) {
            return false; // 이미 언팔 상태 (idempotent)
        }
        followingRepository.deleteByFollowerAndFollowed(me, target);
        return true;
    }
}
