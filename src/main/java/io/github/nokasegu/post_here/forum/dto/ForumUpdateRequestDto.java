package io.github.nokasegu.post_here.forum.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ForumUpdateRequestDto {
    private String content;
    private String musicApiUrl;
    private List<Long> deletedImageIds; // 제할 이미지 ID 목록
}
