package io.github.nokasegu.post_here.follow.repository;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowingRepository extends JpaRepository<FollowingEntity, Long> {

    // 나를 팔로우하는 유저(팔로워 목록)
    @Query("select f.follower " +
            "from FollowingEntity f " +
            "where f.followed.id = :meId " +
            "order by f.follower.nickname asc, f.follower.id asc")
    Page<UserInfoEntity> findFollowersByMeId(@Param("meId") Long meId, Pageable pageable);

    // 내가 팔로우하는 유저(팔로잉 목록)
    @Query("select f.followed " +
            "from FollowingEntity f " +
            "where f.follower.id = :meId " +
            "order by f.followed.nickname asc, f.followed.id asc")
    Page<UserInfoEntity> findFollowingsByMeId(@Param("meId") Long meId, Pageable pageable);

    // 팔로우 여부/언팔에 사용
    boolean existsByFollowerAndFollowed(UserInfoEntity follower, UserInfoEntity followed);

    void deleteByFollowerAndFollowed(UserInfoEntity follower, UserInfoEntity followed);

    // 배치 상태 API용: targetIds 중 내가 팔로잉한 id 목록
    @Query("select f.followed.id " +
            "from FollowingEntity f " +
            "where f.follower.id = :meId and f.followed.id in :targetIds")
    List<Long> findFollowedIdsByMeIn(@Param("meId") Long meId, @Param("targetIds") List<Long> targetIds);

    // 특정 사용자를 팔로우하는 사람의 수 (팔로워 수)
    long countByFollowed(UserInfoEntity followed);

    // 특정 사용자가 팔로우하는 사람의 수 (팔로잉 수)
    long countByFollower(UserInfoEntity follower);
}
