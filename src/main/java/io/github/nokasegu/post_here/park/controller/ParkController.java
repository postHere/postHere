package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.service.ParkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller // View가 아닌 Data(JSON/XML)를 반환하는 컨트롤러
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;

    @GetMapping("/park-write")
    public String parkWrite() {
        return "/park/park-write";
    }

    /**
     * AJAX 요청을 처리하여 특정 유저의 Park 정보를 JSON으로 반환합니다.
     */
    @ResponseBody
    @GetMapping("/profile/park/{nickname}")
    public ResponseEntity<ParkResponseDto> getParkData(@PathVariable String nickname) {
        try {
            ParkResponseDto parkDto = parkService.findParkByOwnerNickname(nickname);
            return ResponseEntity.ok(parkDto);
        } catch (IllegalArgumentException e) {
            // 해당 닉네임의 유저나 Park 정보가 없을 경우 404 Not Found 응답
            return ResponseEntity.notFound().build();
        }
    }
}