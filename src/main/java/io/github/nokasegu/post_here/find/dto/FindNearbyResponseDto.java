package io.github.nokasegu.post_here.find.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * region: 1(열람 가능) || 2(열람 불가),
 * nickname,
 * lat,
 * lng,
 * profile_image_url,
 * find_pk
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindNearbyResponseDto {

    private int region;
    private String nickname;
    private double lat;
    private double lng;
    private String profile_image_url;
    private Long find_pk;
}
