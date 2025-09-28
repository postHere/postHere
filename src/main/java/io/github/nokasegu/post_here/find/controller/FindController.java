package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.find.dto.FindNearbyResponseDto;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.location.dto.LocationRequestDto;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FindController {

    private final FindService findService;
    private final UserInfoService userInfoService;

    @GetMapping("/find")
    public String findController(Model model) {
        log.debug("testtttttttttttttt");
        return "/find/find-write";
    }

    @GetMapping("/around-finds")
    public WrapperDTO<List<FindNearbyResponseDto>> whereAmI(@RequestBody LocationRequestDto location) {

        UserInfoEntity user = userInfoService.getUserInfoByEmail(location.getUser());
        List<FindNearbyResponseDto> findList = findService.getFindsInArea(location.getLng(), location.getLat(), user.getId());

        return WrapperDTO.<List<FindNearbyResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(findList)
                .build();
    }
}
