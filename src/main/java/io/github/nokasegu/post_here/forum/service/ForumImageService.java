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
@Transactional
public class ForumImageService {

    private final ForumImageRepository forumImageRepository;
    private final S3UploaderService s3UploaderService;

    /**
     * 이미지 URL을 받아 DB에 저장하고, 포럼 게시글에 연결
     *
     * @param imageUrl DB에 저장할 이미지 URL
     * @param forum    이미지와 연결할 포럼 엔티티
     * @return DB에 저장된 ForumImageEntity
     */
    public ForumImageEntity saveImage(String imageUrl, ForumEntity forum) {
        ForumImageEntity forumImage = ForumImageEntity.builder()
                .imgUrl(imageUrl)
                .forum(forum) // ForumEntity를 받아 바로 연결
                .build();
        return forumImageRepository.save(forumImage);
    }

    /**
     * 이미지를 S3에 업로드하고 URL을 반환 (DB 저장 없음)
     *
     * @param image MultipartFile 객체
     * @return S3에 업로드된 이미지의 URL
     * @throws IOException
     */
    public String uploadImage(MultipartFile image) throws IOException {
        return s3UploaderService.upload(image, "forum-images");
    }

    /**
     * 포럼에 연결된 모든 이미지 엔티티를 DB와 S3에서 삭제
     *
     * @param forum 이미지를 삭제할 ForumEntity
     */
    public void deleteImages(ForumEntity forum) {
        List<ForumImageEntity> images = forumImageRepository.findByForum(forum);
        for (ForumImageEntity image : images) {
            s3UploaderService.delete(image.getImgUrl());
        }
        forumImageRepository.deleteAll(images);
    }

    /**
     * ID 목록에 해당하는 이미지들을 DB와 S3에서 삭제합니다.
     *
     * @param imageIds 삭제할 이미지 ID 목록
     * @param userId   현재 사용자 ID (권한 확인용)
     */
    public void deleteImagesByIds(List<Long> imageIds, Long userId) {
        List<ForumImageEntity> imagesToDelete = forumImageRepository.findAllById(imageIds);

        for (ForumImageEntity image : imagesToDelete) {
            if (!image.getForum().getWriter().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 이미지에 대한 삭제 권한이 없습니다.");
            }
            s3UploaderService.delete(image.getImgUrl());
        }

        forumImageRepository.deleteAll(imagesToDelete);
    }
}
