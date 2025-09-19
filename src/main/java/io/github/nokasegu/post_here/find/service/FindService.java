package io.github.nokasegu.post_here.find.service;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.find.dto.FindPostSummaryDto;
import io.github.nokasegu.post_here.find.repository.FindRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FindService {

    private final FindRepository findRepository;
    private final UserInfoRepository userInfoRepository;

    /**
     * 특정 사용자가 작성한 Find 게시물 목록을 페이지 단위로 조회
     */
    @Transactional(readOnly = true)
    public Page<FindPostSummaryDto> getMyFinds(String userEmail, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<FindEntity> findsPage = findRepository.findByWriterOrderByIdDesc(user, pageable);

        return findsPage.map(find -> FindPostSummaryDto.builder()
                .id(find.getId())
                .imageUrl(find.getContentCaptureUrl())
                // TODO: 현재 좌표만 저장되어 있으므로, 위치 이름은 일단 "Unknown"으로 처리합니다.
                //       좌표->주소 변환 서비스(Reverse Geocoding)가 있다면 여기서 호출합니다.
                .location("Unknown")
                .isExpiring(find.getExpirationDate() != null && find.getExpirationDate().isAfter(LocalDateTime.now()))
                .build());
    }
}
