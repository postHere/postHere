package io.github.nokasegu.post_here.find.service;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final S3UploaderService s3UploaderService;
    private final GeometryFactory geometryFactory;

    private final Map<Long, Map<Long, Instant>> userNotificationTimestamps = new ConcurrentHashMap<>();

    // --- (이하 기존 코드들은 변경 없이 그대로 유지됩니다) ---

    public List<FindNearbyResponseDto> getFindsInArea(double lng, double lat, Long userId) {

        List<FindNearbyDto> nearbyAll = findRepository.findNearby(lng, lat, userId);

        return nearbyAll.stream()
                .map(dto -> {
                    // 거리에 따라 region 값을 결정합니다 (삼항 연산자 사용).
                    int regionValue = (dto.getDistanceInMeters() <= 50) ? 1 : 2;

                    // FindNearbyDto를 FindNearbyResponseDto로 변환하여 반환합니다.
                    return FindNearbyResponseDto.builder()
                            .find_pk(dto.getFind_pk())
                            .profile_image_url(dto.getProfile_image_url())
                            .nickname(dto.getNickname())
                            .lat(dto.getLat())
                            .lng(dto.getLng())
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
                .location("서울시 강남구") // TODO: 좌표->주소 변환 로직 필요
                .isExpiring(find.getExpirationDate() != null && find.getExpirationDate().isAfter(LocalDateTime.now()))
                .createdAt(find.getCreatedAt())
                .expiresAt(find.getExpirationDate())
                .build());
    }

    /**
     * 닉네임으로 특정 사용자의 Find 게시물 목록을 페이지 단위로 조회
     */
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
                .createdAt(find.getCreatedAt())
                .expiresAt(find.getExpirationDate())
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

    public void saveFind(FindRequestDto findRequestDto, String email) throws IOException {

        UserInfoEntity user = userInfoService.getUserInfoByEmail(email);

        String dirName1 = "find/" + findRequestDto.getLat() + "_" + findRequestDto.getLng();
        String originUrl = s3UploaderService.upload(findRequestDto.getContent_capture(), dirName1);

        String dirName2 = "overwrite/" + findRequestDto.getLat() + "_" + findRequestDto.getLng();
        String overwriteUrl = s3UploaderService.upload(findRequestDto.getContent_capture(), dirName2);

        Point point = geometryFactory.createPoint(new Coordinate(findRequestDto.getLng(), findRequestDto.getLat()));

        findRepository.save(
                FindEntity.builder()
                        .writer(user)
                        .coordinates(point)
                        .contentCaptureUrl(originUrl)
                        .contentOverwriteUrl(overwriteUrl)
                        .expirationDate(makeTime(findRequestDto.getExpiration_date()))
                        .build()
        );
    }

    private LocalDateTime makeTime(String expiredDate) {

        LocalDate datePart = LocalDate.parse(expiredDate);
        LocalTime timePart = LocalTime.of(23, 59, 59);
        return datePart.atTime(timePart);
    }

    public void deleteFind(Long findId) {
        findRepository.deleteById(findId);
    }

    public FindEntity getFindById(Long findId) {
        return findRepository.findById(findId).orElseThrow(
                () -> new EntityNotFoundException("Find NOT FOUND")
        );
    }

    @Transactional
    public void updateFind(Long findId, MultipartFile image) throws IOException {

        FindEntity find = getFindById(findId);

        String dirName = getDirname(find.getContentOverwriteUrl());
        s3UploaderService.delete(find.getContentOverwriteUrl());

        String newUrl = s3UploaderService.upload(image, dirName);
        find.setContentOverwriteUrl(newUrl);
    }

    private String getDirname(String url) {

        String result = null;

        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            int lastIndex = path.lastIndexOf("/");
            result = path.substring(1, lastIndex);

        } catch (URISyntaxException e) {
            log.error("URL 구조가 올바르지 않습니다 : {}", e.getMessage());
        }

        log.info("{}", result);
        return result;
    }

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
