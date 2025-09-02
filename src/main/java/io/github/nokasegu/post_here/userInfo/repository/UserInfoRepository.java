package io.github.nokasegu.post_here.userInfo.repository;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfoEntity, Long> {

    // JPQL을 사용하여 이메일로 유저를 조회하는 메서드
    @Query(value = "SELECT u " +
            "FROM UserInfoEntity u " +
            "WHERE u.email = :email")
    public UserInfoEntity findByLoginId(@Param("email") String email);

    // Spring Data JPA의 메서드 이름 규칙을 사용하여 이메일로 유저를 조회하는 메서드
    // null 방지를 위해 Optional을 반환하는 것을 권장합니다.
    public Optional<UserInfoEntity> findByEmail(String email);
}
