package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.service.ParkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/profile/park")
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;

    /**
     * Park 정보 조회 (GET /profile/park/{profile_id})
     */
    @GetMapping("/{profile_id}")
    public ResponseEntity<ParkResponseDto> getPark(@PathVariable("profile_id") Long profileId) {
        ParkResponseDto responseDto = parkService.getPark(profileId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Park 작성 (POST /profile/park/{profile_id})
     */
    @PostMapping("/{profile_id}")
    public ResponseEntity<String> updatePark(
            @PathVariable("profile_id") Long profileId,
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        Long currentUserId = userDetails.getUserInfo().getId();
        String newImageUrl = parkService.updatePark(profileId, currentUserId, imageFile);
        return ResponseEntity.ok(newImageUrl);
    }

    /**
     * Park 초기화 (기존의 삭제 기능) (DELETE /profile/park/{profile_id})
     */
    @DeleteMapping("/{profile_id}")
    public ResponseEntity<Void> resetPark(
            @PathVariable("profile_id") Long profileId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = userDetails.getUserInfo().getId();
        parkService.resetPark(profileId, currentUserId);
        return ResponseEntity.noContent().build();
    }


    // 테스트용 페이지 맵핑 .. 지워도 됨
    @GetMapping("/test-page")
    public String parkTestPage() {
        // resources/templates/userInfo/park-test.html 을 가리킵니다.
        return "userInfo/park-test";
    }
}