package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
import io.github.nokasegu.post_here.park.dto.ParkResponseDto;
import io.github.nokasegu.post_here.park.service.ParkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ParkAPIController {

    private final ParkService parkService;
    private final S3UploaderService s3UploaderService;


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

    @PostMapping("/profile/park/{nickname}")
    @ResponseBody
    public ResponseEntity<String> createPark(
            @PathVariable String nickname,
            @RequestParam("image") MultipartFile imageFile) {

        // 이미지 파일이 비어있는지 확인
        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body("이미지 파일이 비어있습니다.");
        }

        try {
            // 1. S3UploaderService를 이용해 S3에 이미지 업로드
            // 'park'라는 이름의 디렉토리 안에 저장하도록 지정
            String imageUrl = s3UploaderService.upload(imageFile, "park");
            log.info("S3에 업로드된 이미지 URL: {}", imageUrl);

            // 2. ParkService를 통해 DB에 정보 저장
            parkService.createPark(nickname, imageUrl);

            return ResponseEntity.ok("Park created successfully.");

        } catch (IOException e) {
            log.error("S3 파일 업로드 중 IO 에러 발생", e);
            return ResponseEntity.internalServerError().body("파일 업로드 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Park 생성 중 알 수 없는 에러 발생", e);
            return ResponseEntity.internalServerError().body("Park 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * [추가] 현재 로그인된 사용자의 Park 정보를 JSON으로 반환합니다.
     *
     * @param userDetails Spring Security가 제공하는 현재 사용자 정보
     * @return Park 정보 DTO 또는 404 응답
     */
    @GetMapping("/api/v1/users/me/park")
    public ResponseEntity<ParkResponseDto> getMyParkData(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            // 로그인되지 않은 경우 401 Unauthorized 응답
            return ResponseEntity.status(401).build();
        }
        try {
            // userDetails.getUsername()은 사용자 이메일을 반환합니다.
            ParkResponseDto parkDto = parkService.findParkByOwnerEmail(userDetails.getUsername());
            return ResponseEntity.ok(parkDto);
        } catch (IllegalArgumentException e) {
            // 해당 유저의 Park 정보가 없을 경우 404 Not Found 응답
            return ResponseEntity.notFound().build();
        }
    }
}
