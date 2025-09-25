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
                        ST_X(coordinates) AS lng,
                    	ST_Distance_Sphere(f.coordinates, ST_SRID(POINT(:lng, :lat), 4326)) AS distanceInMeters
                    FROM
                    	find f
                    JOIN user_info u ON f.writer_id = u.user_info_pk
                    HAVING
                    	distanceInMeters <= 500
                    ORDER BY distanceInMeters ASC;
                    """, nativeQuery = true)
    List<FindNearbyDto> findNearby(@Param("lng") double lng, @Param("lat") double lat);

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
                    HAVING
                    	distanceInMeters <= 50
                    ORDER BY distanceInMeters ASC;
                    """, nativeQuery = true)
    List<FindNearbyReadableOnlyDto> findNearbyReadableOnly(@Param("lon") double lon, @Param("lat") double lat);

    Page<FindEntity> findByWriterOrderByIdDesc(UserInfoEntity writer, Pageable pageable);
}
