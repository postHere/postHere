package io.github.nokasegu.post_here.forum.dto;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
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
