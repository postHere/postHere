package io.github.nokasegu.post_here.park.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParkResponseDto {
    private String contentCaptureUrl;

    public ParkResponseDto(String contentCaptureUrl) {
        this.contentCaptureUrl = contentCaptureUrl;
    }
}