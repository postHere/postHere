// src/main/java/io/github/nokasegu/post_here/notification/repository/NotificationRepository.java
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

/**
 * NotificationRepository
 * <p>
 * [역할]
 * - 알림(NotificationEntity)의 목록 조회, 미읽음 개수 조회, 읽음 처리(batch update)를 담당하는 JPA Repository.
 * <p>
 * [사용 위치]
 * - NotificationService.list(...) : (기존) findListByTarget(...) + countByTargetUserAndCheckStatusIsFalse(...)
 * - NotificationService.markRead(...) : markReadByIds(...)
 * - NotificationService.markAllRead(...) : markAllRead(...)
 * <p>
 * [트랜잭션/성능 메모]
 * - @Modifying 메서드는 Service 계층에서 @Transactional 경계 안에서 호출해야 한다.
 * - 배치 업데이트(clearAutomatically=true, flushAutomatically=true)로 1차 캐시/스냅샷 불일치 최소화.
 * - 목록 조회 정렬: createdAt DESC, id DESC (동시 타임스탬프 시 안정적 순서 보장).
 * <p>
 * [확장/제약 메모]
 * - (기존) findListByTarget 쿼리는 following 연관을 INNER JOIN FETCH 하므로 FOLLOW 전용.
 * - (신규) findUnifiedListByTarget 쿼리는 FOLLOW/COMMENT 등 다양한 타입을 한 번에 조회하도록
 * LEFT JOIN 기반으로 확장. Service.list(...)에서 이 메서드를 사용하도록 변경했다.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * [알림 목록 조회 - FOLLOW 전용]
     * - targetUser 기준으로 최신순 페이지 조회.
     * - following 및 follower(행위자)를 즉시 로딩(FETCH)하여 N+1 방지.
     * <p>
     * 제약/주의:
     * - INNER JOIN FETCH n.following 으로 인해 "following 연관이 있는 알림"만 반환됨.
     * (즉, FOLLOW 알림 전용 쿼리)
     * - 유지 목적으로 남겨둠(호환성). 실제 리스트 화면은 아래 통합 쿼리를 사용한다.
     */
    @Query("""
            select n
              from NotificationEntity n
              join fetch n.following f
              join fetch f.follower ff
             where n.targetUser = :target
             order by n.createdAt desc, n.id desc
            """)
    List<NotificationEntity> findListByTarget(@Param("target") UserInfoEntity target, Pageable pageable);

    // ===================== [신규] 통합 목록 조회 =====================
    // - FOLLOW/COMMENT(및 향후 타입) 공통으로 하나의 리스트에 섞어서 내려주기 위한 LEFT JOIN FETCH 쿼리
    // - following/follower, comment/writer 를 미리 로딩하여 N+1 방지
    // - ManyToOne/OneToOne만 fetch하므로 Pageable 사용 가능(중복 행 위험 낮음)
    @Query("""
            select n
              from NotificationEntity n
              left join fetch n.following f
              left join fetch f.follower ff
              left join fetch n.comment c
              left join fetch c.writer cw
             where n.targetUser = :target
             order by n.createdAt desc, n.id desc
            """)
    List<NotificationEntity> findUnifiedListByTarget(@Param("target") UserInfoEntity target, Pageable pageable);

    /**
     * [미읽음 개수 조회]
     */
    long countByTargetUserAndCheckStatusIsFalse(UserInfoEntity target);

    /**
     * [선택 알림 읽음 처리]
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.checkStatus = true where n.targetUser = :target and n.id in :ids")
    int markReadByIds(@Param("target") UserInfoEntity target, @Param("ids") List<Long> ids);

    /**
     * [전체 읽음 처리]
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.checkStatus = true where n.targetUser = :target and n.checkStatus = false")
    int markAllRead(@Param("target") UserInfoEntity target);
}
