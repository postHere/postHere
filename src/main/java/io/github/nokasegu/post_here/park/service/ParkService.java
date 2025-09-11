package io.github.nokasegu.post_here.park.service;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.repository.ParkRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParkService {

    private final ParkRepository parkRepository;
    private final UserInfoRepository userInfoRepository;
    private final FollowingRepository followingRepository;
    private final S3UploaderService s3UploaderService;

    /**
     * 특정 사용자의 Park 정보를 조회합니다. 정보가 없으면 자동으로 생성합니다.
     *
     * @param ownerId Park 소유자의 ID
     * @return ParkResponseDto
     */

    public ParkResponseDto getPark(Long ownerId) {
        // Park 소유자 정보를 먼저 조회합니다.
        UserInfoEntity owner = userInfoRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Park 소유자 정보를 찾을 수 없습니다. ID: " + ownerId));

        // Park 정보를 조회하고, 만약 없다면(orElseGet) 새로 생성합니다.
        ParkEntity park = parkRepository.findByOwnerId(ownerId)
                .orElseGet(() -> createNewParkForOwner(owner));

        return ParkResponseDto.fromEntity(park);
    }

    /**
     * Park 이미지를 수정합니다.
     *
     * @param ownerId       Park 소유자의 ID
     * @param currentUserId 현재 로그인한 사용자의 ID
     * @param imageFile     새로 업로드할 이미지 파일
     * @return 업데이트된 이미지 URL
     */

    public String updatePark(Long ownerId, Long currentUserId, MultipartFile imageFile) throws IOException {
        // 요청을 보낸 사용자와 Park 소유자의 정보를 조회합니다.
        UserInfoEntity currentUser = userInfoRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("요청 사용자 정보를 찾을 수 없습니다. ID: " + currentUserId));
        UserInfoEntity owner = userInfoRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Park 소유자 정보를 찾을 수 없습니다. ID: " + ownerId));

        // 권한 확인: 요청 사용자가 Park 소유자를 팔로우하고 있는지 확인합니다.(실제 기능 구현 시 활성화 필요)
//        boolean isFollowing = followingRepository.existsByFollowerAndFollowed(currentUser, owner);
//        if (!isFollowing) {
//            throw new AccessDeniedException("Park를 수정할 권한이 없습니다. 팔로우가 필요합니다.");
//        }

        // Park 엔티티를 조회합니다. (정보가 없으면 새로 생성)
        ParkEntity park = parkRepository.findByOwnerId(ownerId)
                .orElseGet(() -> createNewParkForOwner(owner));

        // 기존 이미지를 S3에서 삭제합니다. (기본 이미지가 아닐 경우에만)
        String oldImageUrl = park.getContentCaptureUrl();
        if (oldImageUrl != null && !oldImageUrl.equals(s3UploaderService.getDefaultProfileImage())) {
            s3UploaderService.delete(oldImageUrl);
            log.info("S3에서 기존 이미지 삭제 완료: {}", oldImageUrl);
        }

        // 새 이미지를 S3에 업로드합니다.
        String newImageUrl = s3UploaderService.upload(imageFile, "park");
        log.info("S3에 새 이미지 업로드 완료: {}", newImageUrl);

        // DB에 저장된 Park 엔티티의 이미지 URL을 업데이트합니다.
        park.updateUrl(newImageUrl);

        return newImageUrl;
    }

    /**
     * 특정 사용자를 위한 기본 Park 데이터를 생성하고 저장하는 private 메서드입니다.
     *
     * @param owner Park를 소유할 사용자 엔티티
     * @return 새로 생성된 Park 엔티티
     */
    private ParkEntity createNewParkForOwner(UserInfoEntity owner) {
        log.info("{}번 사용자의 Park 데이터가 없어 새로 생성합니다.", owner.getId());
        ParkEntity newPark = ParkEntity.builder()
                .owner(owner)
                .contentCaptureUrl(s3UploaderService.getDefaultProfileImage())
                .build();
        return parkRepository.save(newPark);
    }
    
    public void resetPark(Long ownerId, Long currentUserId) {
        // Park는 본인만 초기화할 수 있도록 로직을 구성합니다.
        if (!ownerId.equals(currentUserId)) {
            throw new AccessDeniedException("자신의 Park만 초기화할 수 있습니다.");
        }

        // Park 엔티티를 조회합니다. (정보가 없으면 새로 생성)
        ParkEntity park = parkRepository.findByOwnerId(ownerId)
                .orElseGet(() -> createNewParkForOwner(userInfoRepository.findById(ownerId)
                        .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))));

        // S3에서 현재 이미지를 삭제합니다. (기본 이미지가 아닐 경우에만)
        String currentImageUrl = park.getContentCaptureUrl();
        if (currentImageUrl != null && !currentImageUrl.equals(s3UploaderService.getDefaultProfileImage())) {
            s3UploaderService.delete(currentImageUrl);
            log.info("S3에서 기존 이미지 삭제 완료: {}", currentImageUrl);
        }

        // Park의 이미지 URL을 기본 이미지 URL로 업데이트(초기화)합니다.
        park.updateUrl(s3UploaderService.getDefaultProfileImage());
        log.info("{}번 사용자의 Park를 기본 이미지로 초기화했습니다.", ownerId);
    }
}