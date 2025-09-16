package io.github.nokasegu.post_here.park.service;

import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.repository.ParkRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParkService {

    private final UserInfoRepository userInfoRepository;
    private final ParkRepository parkRepository;

    /**
     * 유저 닉네임을 기반으로 Park 정보를 조회하는 비즈니스 로직
     */
    @Transactional(readOnly = true)
    public ParkResponseDto findParkByOwnerNickname(String nickname) {
        // 1. 닉네임으로 유저 정보를 조회합니다.
        UserInfoEntity user = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("User not found with nickname: " + nickname));

        // 2. 조회된 유저 정보로 Park 정보를 조회합니다.
        ParkEntity park = parkRepository.findByOwner(user)
                .orElseThrow(() -> new IllegalArgumentException("Park not found for user: " + nickname));

        // 3. ParkEntity에서 URL을 DTO로 변환하여 반환합니다.
        return new ParkResponseDto(park.getContentCaptureUrl());
    }

    // 여기에 Park 생성, 수정, 삭제 등의 다른 서비스 메소드들이 위치할 수 있습니다.
}