package io.github.nokasegu.post_here.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploaderService {

    private final S3Client s3Client;

    // application-secret.yml의 custom.aws.s3.bucket 값을 주입받습니다.
    @Value("${custom.aws.s3.bucket}")
    private String bucket;

    // application-secret.yml의 custom.aws.s3.default-image 값을 주입받습니다.
    @Value("${custom.aws.s3.default-image}")
    private String defaultProfileImageUrl;


    // MultipartFile을 전달받아 S3에 업로드하고 URL을 반환합니다.
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + multipartFile.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(multipartFile.getContentType())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(multipartFile.getBytes()));

        return "https://" + bucket + ".s3.amazonaws.com/" + fileName;
    }

    /**
     * ✅ [추가] S3 버킷에서 파일을 삭제하는 메소드
     *
     * @param fileUrl 삭제할 파일의 전체 URL
     */
    public void delete(String fileUrl) {
        // URL에서 파일 키(파일 경로와 이름)를 추출
        String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    // 기본 이미지 URL을 반환하는 메서드를 추가합니다.
    public String getDefaultProfileImage() {
        return defaultProfileImageUrl;
    }
}
