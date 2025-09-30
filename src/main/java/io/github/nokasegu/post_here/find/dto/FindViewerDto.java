package io.github.nokasegu.post_here.find.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자를 자동으로 만듭니다.
public class FindViewerDto {
    private List<FindDetailDto> posts;   // 스와이프할 모든 게시물 목록
    private int startIndex;              // 시작할 페이지의 인덱스
}

