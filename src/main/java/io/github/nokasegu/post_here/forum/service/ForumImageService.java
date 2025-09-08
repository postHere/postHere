package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.domain.ForumImageEntity;
import io.github.nokasegu.post_here.forum.repository.ForumImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumImageService {

    private final ForumImageRepository forumImageRepository;
    private final S3UploaderService s3UploaderService;

    // 포럼 게시글에 연결된 이미지 URL 목록을 DB에 저장합니다.
    @Transactional
    public void saveImages(ForumEntity forum, List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                ForumImageEntity forumImage = ForumImageEntity.builder()
                        .forum(forum)
                        .imgUrl(imageUrl)
                        .build();
                forumImageRepository.save(forumImage);
            }
        }
    }

    // S3 업로드를 전담하는 메서드를 추가합니다.
    public String uploadImage(MultipartFile image, String dirName) throws IOException {
        return s3UploaderService.upload(image, dirName);
    }

}
