package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.service.ParkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;

    /**
     * Park 정보 조회 API
     *
     * @param ownerId 조회할 Park의 소유자 ID
     */
    @GetMapping("/api/park/{ownerId}")
    public ResponseEntity<ParkResponseDto> getPark(@PathVariable Long ownerId) {
        ParkResponseDto responseDto = parkService.getPark(ownerId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Park 이미지 수정 API
     *
     * @param ownerId     수정할 Park의 소유자 ID
     * @param imageFile   새로 등록할 이미지 파일
     * @param userDetails 현재 로그인한 유저의 정보 (Principal 객체)
     */
    @PostMapping("/api/park/{ownerId}") // 리소스의 부분 수정이지만, 이미지 파일 처리를 위해 POST 또는 PUT 사용
    public ResponseEntity<String> updatePark(
            @PathVariable Long ownerId,
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails // Spring Security의 Principal 객체로 현재 유저 정보 가져오기
    ) throws IOException {

        // @AuthenticationPrincipal을 통해 로그인한 사용자의 ID를 가져옵니다.
        Long currentUserId = userDetails.getUserInfo().getId();

        String newImageUrl = parkService.updatePark(ownerId,
                currentUserId,
                imageFile);

        return ResponseEntity.ok(newImageUrl);
    }

    // 테스트용 페이지 맵핑 .. 지워도 됨
    @GetMapping("/test-page")
    public String parkTestPage() {
        // resources/templates/userInfo/park-test.html 을 가리킵니다.
        return "userInfo/park-test";
    }
}