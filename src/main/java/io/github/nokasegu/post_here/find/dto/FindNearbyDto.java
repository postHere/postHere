package io.github.nokasegu.post_here.find.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FindNearbyDto {

    private Long find_pk;
    private String nickname;
    private String profile_image_url;
    private Double lng;
    private Double lat;
    private Double distanceInMeters;
}
