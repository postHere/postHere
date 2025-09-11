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
 * - NotificationService.list(...) : findListByTarget(...) + countByTargetUserAndCheckStatusIsFalse(...)
 * - NotificationService.markRead(...) : markReadByIds(...)
 * - NotificationService.markAllRead(...) : markAllRead(...)
 * <p>
 * [트랜잭션/성능 메모]
 * - @Modifying 메서드는 Service 계층에서 @Transactional 경계 안에서 호출해야 한다.
 * - 배치 업데이트(clearAutomatically=true, flushAutomatically=true)로 1차 캐시/스냅샷 불일치 최소화.
 * - 목록 조회 정렬: createdAt DESC, id DESC (동시 타임스탬프 시 안정적 순서 보장).
 * <p>
 * [확장/제약 메모]
 * - 현재 findListByTarget 쿼리는 following 연관을 INNER JOIN FETCH 하므로
 * "팔로우 알림"에 한정된 결과를 조회한다(FOLLOW 전용). 댓글/기타 알림을 포함하려면 쿼리 분기/동적 쿼리 고려.
 * - Pageable 사용 시 Hibernate는 FETCH JOIN과 함께 페이징을 지원하지만(버전에 따라 경고 가능),
 * 다대일/일대일 관계라면 중복 행 위험은 낮다. 필요 시 DISTINCT 추가를 검토.
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
     * - 다른 유형(COMMENT/등)을 함께 보여주려면 별도 쿼리 또는 UNION 전략 필요.
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

    /**
     * [미읽음 개수 조회]
     * - 네비게이션 배지(빨간 점) 및 목록 화면 상단 표시에 사용.
     */
    long countByTargetUserAndCheckStatusIsFalse(UserInfoEntity target);

    /**
     * [선택 알림 읽음 처리]
     * - ids에 포함된 알림들만 읽음(checkStatus=true)으로 마킹.
     * <p>
     * 전제:
     * - Service 단에서 ids 비어있음(empty) 호출 금지(IN () 방지).
     * - @Transactional 경계 내에서 호출.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.checkStatus = true where n.targetUser = :target and n.id in :ids")
    int markReadByIds(@Param("target") UserInfoEntity target, @Param("ids") List<Long> ids);

    /**
     * [전체 읽음 처리]
     * - targetUser 소유의 모든 미읽음 알림을 일괄 읽음으로 마킹.
     * <p>
     * 전제:
     * - @Transactional 경계 내에서 호출.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.checkStatus = true where n.targetUser = :target and n.checkStatus = false")
    int markAllRead(@Param("target") UserInfoEntity target);
}
