package io.github.nokasegu.post_here.park.dto;

import io.github.nokasegu.post_here.park.domain.ParkEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ParkResponseDto {

    private final Long parkId;
    private final Long ownerId;
    private final String contentCaptureUrl;

    @Builder
    public ParkResponseDto(Long parkId, Long ownerId, String contentCaptureUrl) {
        this.parkId = parkId;
        this.ownerId = ownerId;
        this.contentCaptureUrl = contentCaptureUrl;
    }

    public static ParkResponseDto fromEntity(ParkEntity park) {
        return ParkResponseDto.builder()
                .parkId(park.getId())
                .ownerId(park.getOwner().getId())
                .contentCaptureUrl(park.getContentCaptureUrl())
                .build();
    }
}

