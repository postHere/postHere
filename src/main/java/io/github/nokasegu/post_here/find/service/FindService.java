package io.github.nokasegu.post_here.find.service;

import io.github.nokasegu.post_here.find.dto.FindNearbyDto;
import io.github.nokasegu.post_here.find.dto.FindNearbyReadableOnlyDto;
import io.github.nokasegu.post_here.find.dto.FindNearbyResponseDto;
import io.github.nokasegu.post_here.find.repository.FindRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindService {

    private final FindRepository findRepository;

    public List<FindNearbyResponseDto> getFindsInArea(double lng, double lat) {

        List<FindNearbyDto> nearbyAll = findRepository.findNearby(lng, lat);

        return nearbyAll.stream()
                .map(dto -> {
                    // 거리에 따라 region 값을 결정합니다 (삼항 연산자 사용).
                    String regionValue = (dto.getDistanceInMeters() <= 50) ? "1" : "2";

                    // FindNearbyDto를 FindNearbyResponseDto로 변환하여 반환합니다.
                    return FindNearbyResponseDto.builder()
                            .find_pk(dto.getFind_pk().toString())
                            .profile_image_url(dto.getProfile_image_url())
                            .nickname(dto.getNickname())
                            .lat(dto.getLat().toString())
                            .lng(dto.getLng().toString())
                            .region(regionValue)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public void checkFindReadable(double lng, double lat) {

        List<FindNearbyReadableOnlyDto> result = findRepository.findNearbyReadableOnly(lng, lat);
        int amount = result.size();
        //메세지 양식 : {nickname}님 외 {amount}명의 fin'd가 존재합니다
    }
}
