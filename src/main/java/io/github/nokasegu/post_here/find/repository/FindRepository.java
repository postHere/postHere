package io.github.nokasegu.post_here.find.repository;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.find.dto.FindNearbyDto;
import io.github.nokasegu.post_here.find.dto.FindNearbyReadableOnlyDto;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FindRepository extends JpaRepository<FindEntity, Long> {

    // 쿼리는 그대로 두고, 반환 타입만 List<FindNearbyDto>로 변경합니다.
    @Query(
            value = """
                    SELECT
                        f.find_pk AS find_pk,
                        u.nickname AS nickname,
                        u.profile_photo_url AS profile_image_url,
                        ST_Y(f.coordinates) AS lat,
                        ST_X(f.coordinates) AS lng,
                        ST_Distance_Sphere(f.coordinates, ST_SRID(POINT(:lng, :lat), 4326)) AS distanceInMeters
                    FROM
                        find f
                    JOIN user_info u ON f.writer_id = u.user_info_pk
                    JOIN following fw ON f.writer_id = fw.followed_id
                    WHERE
                        fw.follower_id = :userId AND f.expiration_date >= NOW()
                    HAVING
                        distanceInMeters <= 200
                    ORDER BY
                        distanceInMeters ASC
                    """, nativeQuery = true)
    List<FindNearbyDto> findNearby(@Param("lng") double lng, @Param("lat") double lat, @Param("userId") Long userId);
    // ▼▼▼ [변경] 500m → 200m 로 범위 축소 (프런트/기획 반영)

    @Query(
            value = """
                    SELECT
                    	f.find_pk AS find_pk,
                        u.nickname AS nickname,
                    	u.profile_photo_url AS profile_image_url,
                    	ST_Distance_Sphere(f.coordinates, ST_SRID(POINT(:lon, :lat), 4326)) AS distanceInMeters
                    FROM
                    	find f
                    JOIN user_info u ON f.writer_id = u.user_info_pk
                    JOIN following fw ON f.writer_id = fw.followed_id
                    WHERE
                    	fw.follower_id = :userId AND f.expiration_date >= NOW()
                    HAVING
                    	distanceInMeters <= 50
                    ORDER BY distanceInMeters ASC;
                    """, nativeQuery = true)
    List<FindNearbyReadableOnlyDto> findNearbyReadableOnly(@Param("lon") double lon, @Param("lat") double lat, @Param("userId") Long userId);

    Page<FindEntity> findByWriterOrderByIdDesc(UserInfoEntity writer, Pageable pageable);

    // ▼▼▼ [스와이프 뷰어용] 새로운 메소드를 여기에 추가했습니다. ▼▼▼
    @Query("SELECT f FROM FindEntity f JOIN FETCH f.writer WHERE f.writer = :writer ORDER BY f.createdAt DESC")
    List<FindEntity> findAllByWriterWithDetails(@Param("writer") UserInfoEntity writer);

    // ▼▼▼ [추가됨] 자동 삭제를 위해 만료 시간이 지난 게시물을 찾는 메소드 ▼▼▼
    List<FindEntity> findAllByExpirationDateBefore(LocalDateTime now);
}
