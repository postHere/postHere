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

@Service
@RequiredArgsConstructor
@Transactional
public class ForumImageService {

    private final ForumImageRepository forumImageRepository;
    private final S3UploaderService s3UploaderService;

    /**
     * 이미지를 S3에 업로드하고 URL을 반환합니다. (DB 저장 없음)
     *
     * @param image MultipartFile 객체
     * @return S3에 업로드된 이미지의 URL
     * @throws IOException
     */
    public String uploadImage(MultipartFile image) throws IOException {
        return s3UploaderService.upload(image, "forum-images");
    }

    /**
     * 이미지 URL을 받아 DB에 저장하고, 포럼 게시글에 연결합니다.
     *
     * @param imageUrl DB에 저장할 이미지 URL
     * @param forum    이미지와 연결할 포럼 엔티티
     * @return DB에 저장된 ForumImageEntity
     */
    public ForumImageEntity saveImage(String imageUrl, ForumEntity forum) {
        ForumImageEntity forumImage = ForumImageEntity.builder()
                .imgUrl(imageUrl)
                .forum(forum) // ★★★ 변경: ForumEntity를 받아 바로 연결합니다. ★★★
                .build();
        return forumImageRepository.save(forumImage);
    }
}
