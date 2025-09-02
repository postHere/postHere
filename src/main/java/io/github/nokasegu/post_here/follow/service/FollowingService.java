package io.github.nokasegu.post_here.follow.service;

import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * ✅ 닉네임 부분일치 검색(로그인 사용자 제외), 정렬은 레포지토리 JPQL에서 수행
     */
    @Transactional(readOnly = true)
    public Page<UserInfoEntity> getUsersByNickname(Long meId, String keyword, Pageable pageable) {
        String k = (keyword == null) ? "" : keyword.trim();
        return userInfoRepository.findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(meId, k, pageable);
    }
}
