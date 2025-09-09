package io.github.nokasegu.post_here.forum.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

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

    private List<MultipartFile> images;

    private String spotifyTrackId;

    private String userEmail;
}
