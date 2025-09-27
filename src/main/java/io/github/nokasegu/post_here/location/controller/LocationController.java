package io.github.nokasegu.post_here.location.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.common.util.GeocodingUtil;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.location.dto.LocationRequestDto;
import io.github.nokasegu.post_here.location.dto.LocationResponseDto;
import io.github.nokasegu.post_here.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트를 위한 거시기
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LocationController {

    private final GeocodingUtil geocodingUtil;
    private final LocationService locationService;
    private final FindService findService;

    @PostMapping("/location")
    public WrapperDTO<LocationResponseDto> whereAmI(@RequestBody LocationRequestDto location, @AuthenticationPrincipal UserDetails user) {

        String address = geocodingUtil.getAddressFromCoordinates(location.getLng(), location.getLat());
        log.info("address: {} {} {}", location.getLng(), location.getLat(), address);

        ForumAreaEntity area = locationService.getForumArea(address);
        findService.checkFindReadable(location.getLng(), location.getLat(), user.getUsername());

        return WrapperDTO.<LocationResponseDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(LocationResponseDto.builder()
                        .forumKey(area.getId().toString())
                        .forumName(area.getAddress())
                        .build())
                .build();
    }
}
