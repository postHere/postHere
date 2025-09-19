package io.github.nokasegu.post_here.find.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindPostSummaryDto {
    private Long id;
    private String imageUrl;
    private String location; // TODO: 좌표를 주소로 변환하는 로직 필요
    private boolean isExpiring;
}