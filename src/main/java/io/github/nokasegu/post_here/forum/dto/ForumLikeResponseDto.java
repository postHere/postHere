package io.github.nokasegu.post_here.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ForumLikeResponseDto {

    private int totalLikes;
    private List<String> recentLikerPhotos;
    private boolean isLiked; // 현재 사용자의 좋아요 여부

}
