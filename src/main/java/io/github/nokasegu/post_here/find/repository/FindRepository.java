package io.github.nokasegu.post_here.find.repository;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FindRepository extends JpaRepository<FindEntity, Long> {
    Page<FindEntity> findByWriterOrderByIdDesc(UserInfoEntity writer, Pageable pageable);
}
