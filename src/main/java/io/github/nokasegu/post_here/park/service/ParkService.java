package io.github.nokasegu.post_here.park.service;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.repository.ParkRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParkService {

    private final UserInfoRepository userInfoRepository;
    private final ParkRepository parkRepository;
    private final S3UploaderService s3UploaderService;

    /**
     * 유저 닉네임을 기반으로 Park 정보를 조회하는 비즈니스 로직
     */
    @Transactional
    public void createPark(String nickname, String imageUrl) {
        // 1. 닉네임으로 유저 정보 조회
        UserInfoEntity owner = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 닉네임의 유저를 찾을 수 없습니다: " + nickname));

        // 2. 해당 유저의 기존 Park 데이터 모두 조회
        List<ParkEntity> existingParks = parkRepository.findByOwner(owner);

        // 3. 기존 Park 데이터가 있다면 S3 이미지와 DB 데이터 삭제
        if (!existingParks.isEmpty()) {
            log.info("{} 님의 기존 Park {}개를 삭제합니다.", nickname, existingParks.size());
            // 3-1. S3에서 모든 기존 이미지 삭제
            for (ParkEntity park : existingParks) {
                s3UploaderService.delete(park.getContentCaptureUrl());
            }
            // 3-2. DB에서 모든 기존 데이터 한번에 삭제
            parkRepository.deleteAll(existingParks);
        }

        // 4. 새로운 ParkEntity 생성 및 저장
        ParkEntity newPark = ParkEntity.builder()
                .owner(owner)
                .contentCaptureUrl(imageUrl)
                .build();
        parkRepository.save(newPark);
    }

    /**
     *
     */
    @Transactional(readOnly = true)
    public ParkResponseDto findParkByOwnerNickname(String nickname) {
        UserInfoEntity user = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("User not found with nickname: " + nickname));

        // 이제 유저당 Park는 1개만 존재하므로, 단순 조회가 가능
        ParkEntity park = parkRepository.findOneByOwner(user)
                .orElseThrow(() -> new IllegalArgumentException("Park not found for user: " + nickname));

        return new ParkResponseDto(park.getContentCaptureUrl());
    }

}