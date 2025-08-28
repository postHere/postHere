package io.github.nokasegu.post_here.userInfo.repository;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfoEntity, Long> {

    @Query(value = "SELECT u " +
            "FROM UserInfoEntity u " +
            "WHERE u.loginId = :loginId")
    public UserInfoEntity findByLoginId(@Param("loginId") String loginId);


}
