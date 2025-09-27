package io.github.nokasegu.post_here.find.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FindNearbyReadableOnlyDto {

    private Long find_pk;
    private String nickname;
    private String profile_image_url;
    private double distanceInMeters;
}
