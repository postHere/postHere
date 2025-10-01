package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.service.ParkService;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller // View가 아닌 Data(JSON/XML)를 반환하는 컨트롤러
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;
    private final UserInfoRepository userInfoRepository;

    @GetMapping("/park/{nickname}")
    public String parkWrite(@PathVariable("nickname") String nickname, Model model) {

        // 7. 모델에 최종 닉네임 담기
        model.addAttribute("nickname", nickname);

        // 기존 Park 데이터 조회를 시도
        // ParkService를 통해 기존 Park 정보(URL)를 가져옴
        ParkResponseDto parkDto = parkService.findParkByOwnerNickname(nickname);

        // 모델에 기존 이미지 URL 추가
        model.addAttribute("park_url", parkDto.getContentCaptureUrl());

        return "/park/park-write";
    }
}