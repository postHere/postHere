package io.github.nokasegu.post_here.notification.repository;

import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
               select n
                 from NotificationEntity n
                 join fetch n.following f
                 join fetch f.follower ff
                where n.targetUser = :target
                order by n.createdAt desc, n.id desc
            """)
    List<NotificationEntity> findListByTarget(@Param("target") UserInfoEntity target, Pageable pageable);

    long countByTargetUserAndCheckStatusIsFalse(UserInfoEntity target);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.checkStatus = true where n.targetUser = :target and n.id in :ids")
    int markReadByIds(@Param("target") UserInfoEntity target, @Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.checkStatus = true where n.targetUser = :target and n.checkStatus = false")
    int markAllRead(@Param("target") UserInfoEntity target);
}
