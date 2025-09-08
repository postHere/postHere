package io.github.nokasegu.post_here.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ForumCreateRequestDto {

    private Long writerId;

    private String content;

    private Long location;

    private List<String> imageUrls;

    private String spotifyTrackId;

    private String userEmail;
}
