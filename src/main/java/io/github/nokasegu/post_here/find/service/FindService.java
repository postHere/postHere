package io.github.nokasegu.post_here.find.service;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.find.dto.*;
import io.github.nokasegu.post_here.find.repository.FindRepository;
import io.github.nokasegu.post_here.notification.service.FcmSenderService;
import io.github.nokasegu.post_here.notification.service.NotificationService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindService {

    private final FindRepository findRepository;
    private final UserInfoRepository userInfoRepository;
    private final FcmSenderService fcmSenderService;
    private final UserInfoService userInfoService;
    private final NotificationService notificationService;

    private final Map<Long, Map<Long, Instant>> userNotificationTimestamps = new ConcurrentHashMap<>();

    // --- (이하 기존 코드들은 변경 없이 그대로 유지됩니다) ---

    public List<FindNearbyResponseDto> getFindsInArea(double lng, double lat, Long userId) {

        List<FindNearbyDto> nearbyAll = findRepository.findNearby(lng, lat, userId);

        return nearbyAll.stream()
                .map(dto -> {
                    String regionValue = (dto.getDistanceInMeters() <= 50) ? "1" : "2";
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

    public void checkFindReadable(double lng, double lat, String userEmail) {
        UserInfoEntity user = userInfoService.getUserInfoByEmail(userEmail);
        List<FindNearbyReadableOnlyDto> nearbyFinds = findRepository.findNearbyReadableOnly(lng, lat, user.getId());

        if (nearbyFinds.isEmpty()) {
            log.info("사용자 {} 주변에 새로운 Fin'd가 없습니다.", user.getNickname());
            return;
        }

        int count = 0;
        String nickname = null;

        for (FindNearbyReadableOnlyDto find : nearbyFinds) {
            boolean alreadyNotified = hasBeenNotifiedRecently(user.getId(), find.getFind_pk());
            if (!alreadyNotified) {
                count++;
                nickname = find.getNickname();
                recordNotification(user.getId(), find.getFind_pk());
            } else {
                log.info("{}에게 {}번 알림은 이미 보냄.", user.getNickname(), find.getFind_pk());
            }
        }

        if (count > 0) {
            String message = nickname + "님 외 " + count + "명의 fin'd가 존재합니다";
            notificationService.createFind(user, message);
            fcmSenderService.sendFindNotification(user, message);
        }
    }

    @Transactional(readOnly = true)
    public Page<FindPostSummaryDto> getMyFinds(String userEmail, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Page<FindEntity> findsPage = findRepository.findByWriterOrderByIdDesc(user, pageable);
        return findsPage.map(find -> FindPostSummaryDto.builder()
                .id(find.getId())
                .imageUrl(find.getContentCaptureUrl())
                .location("서울시 강남구") // TODO: 좌표->주소 변환 로직 필요
                .isExpiring(find.getExpirationDate() != null && find.getExpirationDate().isAfter(LocalDateTime.now()))
                .build());
    }

    @Transactional(readOnly = true)
    public Page<FindPostSummaryDto> getFindsByNickname(String nickname, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Page<FindEntity> findsPage = findRepository.findByWriterOrderByIdDesc(user, pageable);
        return findsPage.map(find -> FindPostSummaryDto.builder()
                .id(find.getId())
                .imageUrl(find.getContentCaptureUrl())
                .location("서울시 강남구") // TODO: 좌표->주소 변환 로직 필요
                .isExpiring(find.getExpirationDate() != null && find.getExpirationDate().isAfter(LocalDateTime.now()))
                .build());
    }

    private boolean hasBeenNotifiedRecently(Long userId, Long findId) {
        Map<Long, Instant> userNotifications = userNotificationTimestamps.get(userId);
        if (userNotifications == null) return false;
        Instant lastNotificationTime = userNotifications.get(findId);
        if (lastNotificationTime == null) return false;
        return lastNotificationTime.isAfter(Instant.now().minus(1, ChronoUnit.HOURS));
    }

    private void recordNotification(Long userId, Long findId) {
        userNotificationTimestamps
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(findId, Instant.now());
    }

    /**
     * ▼▼▼ [추가됨] Fin'd 스크롤 피드 상세 페이지에 필요한 데이터를 조회하는 메소드 ▼▼▼
     */
    @Transactional(readOnly = true)
    public List<FindDetailViewDto> getFindFeed(Long startFindId, Long currentUserId) {
        // 1. 클릭한 게시물을 기준으로 작성자를 찾습니다.
        FindEntity startFind = findRepository.findById(startFindId)
                .orElseThrow(() -> new EntityNotFoundException("Fin'd 게시물을 찾을 수 없습니다."));
        UserInfoEntity writer = startFind.getWriter();

        // 2. 해당 작성자의 모든 게시물을 최신순으로 가져옵니다.
        List<FindEntity> allFindsByWriter = findRepository.findAllByWriterWithDetails(writer);

        // 3. 클릭한 게시물이 목록의 몇 번째인지 찾습니다.
        int startIndex = -1;
        for (int i = 0; i < allFindsByWriter.size(); i++) {
            if (allFindsByWriter.get(i).getId().equals(startFindId)) {
                startIndex = i;
                break;
            }
        }

        // 4. 클릭한 게시물부터 시작하여 그보다 오래된 게시물들만 순서대로 새 목록에 담습니다.
        List<FindEntity> orderedFeed = new ArrayList<>();
        if (startIndex != -1) {
            orderedFeed.addAll(allFindsByWriter.subList(startIndex, allFindsByWriter.size()));
        }

        // 5. 최종 목록을 DTO로 변환하여 반환합니다.
        return orderedFeed.stream()
                .map(entity -> new FindDetailViewDto(entity, currentUserId))
                .collect(Collectors.toList());
    }
}

