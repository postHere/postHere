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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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

    public List<FindNearbyResponseDto> getFindsInArea(double lng, double lat, Long userId) {

        List<FindNearbyDto> nearbyAll = findRepository.findNearby(lng, lat, userId);

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
                .location("Unknown") // TODO: 좌표->주소 변환 로직 필요
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
                .location("Unknown") // TODO: 좌표->주소 변환 로직 필요
                .isExpiring(find.getExpirationDate() != null && find.getExpirationDate().isAfter(LocalDateTime.now()))
                .build());
    }


    /**
     * 특정 사용자에게 특정 fin'd에 대한 알림이 최근에 보내졌는지 확인
     */
    private boolean hasBeenNotifiedRecently(Long userId, Long findId) {
        Map<Long, Instant> userNotifications = userNotificationTimestamps.get(userId);
        if (userNotifications == null) return false;

        Instant lastNotificationTime = userNotifications.get(findId);
        if (lastNotificationTime == null) return false;

        // 1시간 이내에 알림을 보냈는지 확인
        return lastNotificationTime.isAfter(Instant.now().minus(1, ChronoUnit.HOURS));
    }

    /**
     * 알림을 보낸 기록을 저장
     */
    private void recordNotification(Long userId, Long findId) {
        userNotificationTimestamps
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(findId, Instant.now());
    }

    @Transactional(readOnly = true)
    public FindViewerDto getFindsForViewer(Long startFindId, Long currentUserId) {
        FindEntity startFind = findRepository.findById(startFindId)
                .orElseThrow(() -> new EntityNotFoundException("Fin'd 게시물을 찾을 수 없습니다."));
        UserInfoEntity writer = startFind.getWriter();

        List<FindEntity> allFindsByWriter = findRepository.findAllByWriterWithDetails(writer);

        List<FindDetailDto> detailDtos = allFindsByWriter.stream()
                .map(findEntity -> {
                    boolean isAuthor = currentUserId != null && findEntity.getWriter().getId().equals(currentUserId);

                    LocalDateTime expirationTime = findEntity.getExpirationDate() != null ? findEntity.getExpirationDate() : findEntity.getCreatedAt().plusHours(24);
                    Duration duration = Duration.between(LocalDateTime.now(), expirationTime);
                    String remainingTimeStr;

                    if (duration.isNegative()) {
                        remainingTimeStr = "만료됨";
                    } else {
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();
                        remainingTimeStr = String.format("%02d:%02d 남음", hours, minutes);
                    }

                    return FindDetailDto.builder()
                            .findId(findEntity.getId())
                            .writerNickname(findEntity.getWriter().getNickname())
                            .writerProfilePhotoUrl(findEntity.getWriter().getProfilePhotoUrl())
                            .contentCaptureUrl(findEntity.getContentCaptureUrl())
                            .locationName("서울시 강남구") // TODO: 좌표->주소 변환
                            .isAuthor(isAuthor)
                            .createdAt(findEntity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")))
                            .remainingTime(remainingTimeStr)
                            .build();
                })
                .collect(Collectors.toList());

        int startIndex = IntStream.range(0, detailDtos.size())
                .filter(i -> detailDtos.get(i).getFindId().equals(startFindId))
                .findFirst()
                .orElse(0);

        return new FindViewerDto(detailDtos, startIndex);
    }

    @Transactional
    public void deleteFind(Long findId, Long currentUserId) {
        FindEntity findEntity = findRepository.findById(findId)
                .orElseThrow(() -> new EntityNotFoundException("Fin'd 게시물을 찾을 수 없습니다."));

        if (currentUserId == null || !findEntity.getWriter().getId().equals(currentUserId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        findRepository.delete(findEntity);
    }
}
