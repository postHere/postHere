package io.github.nokasegu.post_here.find.repository;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FindRepository extends JpaRepository<FindEntity, Long> {
}
