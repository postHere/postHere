package io.github.nokasegu.post_here.find.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FindPostSummaryDto {
    private Long id;
    private String imageUrl;
    private String location; // TODO: 좌표를 주소로 변환하는 로직 필요
    private boolean isExpiring;

    // 필요한 원본 시간 데이터 추가
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}