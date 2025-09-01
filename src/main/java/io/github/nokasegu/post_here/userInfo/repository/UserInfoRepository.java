package io.github.nokasegu.post_here.userInfo.repository;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfoEntity, Long> {

    /**
     * ✅ 기존 쿼리 유지: 이메일로 사용자 조회 (메서드명은 findByLoginId 지만 쿼리는 email 기준)
     */
    @Query("SELECT u FROM UserInfoEntity u WHERE u.email = :email")
    UserInfoEntity findByLoginId(@Param("email") String email);

    /**
     * ✅ 추가: 닉네임 부분일치 + 로그인 사용자 제외, 정렬: nickname asc, id asc
     */
    @Query("""
            select u
              from UserInfoEntity u
             where u.id <> :meId
               and lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%'))
             order by u.nickname asc, u.id asc
            """)
    Page<UserInfoEntity> findByNicknameContainingAndIdNotOrderByNicknameAscIdAsc(
            @Param("meId") Long meId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 이메일로 사용자를 찾는 메서드
    Optional<UserInfoEntity> findByEmail(String email);

    // 닉네임으로 사용자를 찾는 메서드
    Optional<UserInfoEntity> findByNickname(String nickname);
}
