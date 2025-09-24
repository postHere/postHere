package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.find.dto.FindNearbyResponseDto;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.location.dto.LocationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/test")
public class FindController {

    private final FindService findService;

    @GetMapping("/around-finds")
    public WrapperDTO<List<FindNearbyResponseDto>> whereAmI(@RequestBody LocationRequestDto location) {

        List<FindNearbyResponseDto> findList = findService.getFindsInArea(location.getLng(), location.getLat());

        return WrapperDTO.<List<FindNearbyResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(findList)
                .build();
    }


}
