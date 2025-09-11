package io.github.nokasegu.post_here.userInfo.controller;

import io.github.nokasegu.post_here.userInfo.dto.UserInfoDto;
import io.github.nokasegu.post_here.userInfo.service.EmailService;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserInfoController {

    private final UserInfoService userInfoService;
    private final EmailService emailService;

    @GetMapping("/login")
    public String loginPage() {
        return "userInfo/login";
    }

    /**
     * 회원가입 페이지를 보여주는 컨트롤러
     *
     * @return "signup" 뷰 이름
     */
    @GetMapping("/signup")
    public String signupPage() {
        return "userInfo/signup";
    }

    /**
     * 회원가입을 실행하는 컨트롤러
     *
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호
     * @param nickname 사용자 닉네임
     * @return 로그인 페이지로 리디렉션
     */
    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String nickname) {
        log.debug("사용자 정보{}, {}, {}", email, password, nickname);
        userInfoService.signUp(email, password, nickname);

        // 회원가입 성공 후 로그인 페이지로 이동시킵니다.
        return "redirect:/login";
    }

    /*--- API Endpoints for AJAX communication ---*/

    /**
     * 이메일 중복 여부를 확인하는 API
     *
     * @param email 확인할 이메일
     * @return 사용 가능 여부를 담은 Map 객체 (JSON)
     */
    @GetMapping("/api/check-email")
    @ResponseBody // HTML 뷰가 아닌 데이터(JSON)를 반환
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean isAvailable = userInfoService.isEmailAvailable(email);
        return Collections.singletonMap("available", isAvailable);
    }

    /**
     * 닉네임 중복 여부를 확인하는 API
     *
     * @param nickname 확인할 닉네임
     * @return 사용 가능 여부를 담은 Map 객체 (JSON)
     */
    @GetMapping("/api/check-nickname")
    @ResponseBody
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = userInfoService.isNicknameAvailable(nickname);
        return Collections.singletonMap("available", isAvailable);
    }

    /**
     * 이메일 인증 코드를 발송하는 API
     *
     * @param email   코드를 보낼 이메일
     * @param session 인증 코드를 임시 저장할 세션
     * @return 성공 메시지를 담은 Map 객체
     */
    @PostMapping("/api/send-verification")
    @ResponseBody
    public Map<String, Boolean> sendVerificationCode(@RequestParam String email, HttpSession session) {
        try {
            // ✅ 6자리 랜덤 숫자 인증 코드 생성
            Random random = new Random();
            int code = 100000 + random.nextInt(900000);
            String verificationCode = String.valueOf(code);

            // ✅ 이메일 발송
            emailService.sendVerificationEmail(email, verificationCode);

            // ✅ 세션에 인증 코드와 만료 시간 저장 (유효시간 3분)
            long expiryTime = System.currentTimeMillis() + (3 * 60 * 1000);
            session.setAttribute("verificationCode", verificationCode);
            session.setAttribute("expiryTime", expiryTime);
            session.setAttribute("verifiedEmail", email); // 어떤 이메일에 대한 코드인지 저장

            return Collections.singletonMap("success", true);
        } catch (Exception e) {
            // 에러 로깅
            return Collections.singletonMap("success", false);
        }
    }

    /**
     * 이메일 인증 코드를 검증하는 API
     *
     * @param code    사용자가 입력한 인증 코드
     * @param session 세션에 저장된 코드와 비교
     * @return 코드 유효 여부를 담은 Map 객체
     */
    @PostMapping("/api/verify-code")
    @ResponseBody
    public Map<String, Boolean> verifyCode(@RequestParam String code, HttpSession session) {
        String sessionCode = (String) session.getAttribute("verificationCode");
        Long expiryTime = (Long) session.getAttribute("expiryTime");

        // ✅ 시간 만료 또는 코드 불일치 확인
        if (sessionCode == null || expiryTime == null || System.currentTimeMillis() > expiryTime) {
            session.removeAttribute("verificationCode");
            session.removeAttribute("expiryTime");
            session.removeAttribute("verifiedEmail");
            return Collections.singletonMap("valid", false);
        }

        boolean isValid = sessionCode.equals(code);
        if (isValid) {
            // 인증 성공 시, 세션에서 코드 정보는 삭제하되,
            // 어떤 이메일이 인증되었는지는 남겨둘 수 있습니다 (선택사항).
            session.removeAttribute("verificationCode");
            session.removeAttribute("expiryTime");
        }

        return Collections.singletonMap("valid", isValid);
    }

    /**
     * 내 프로필 접근
     *
     * @param @AuthenticationPrincipal UserDetails
     * @param model
     * @return profile HTML
     */
    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal UserDetails user, Model model) {

        String userEmail = user.getUsername();
        UserInfoDto userProfile = userInfoService.getUserProfileByEmail(userEmail);

        model.addAttribute("user", userProfile);
        return "userInfo/profile";
    }

    /**
     * 다른 사람의 프로필에 접근할 때 필요한 API
     * 미완임 프로필 기능 완성 후 마저 완성 정민이 누나가 만들 예정
     */
    @GetMapping("/profile/{userId}")
    public String profile(@PathVariable Long userId, Model model) {
        model.addAttribute("profileUserId", userId);
        // 프로필 페이지가 아직 없는데 팔로우/언팔로우 기능 구현되나 보려고 만들었슴(정민)
        return "userInfo/profile";
    }


    /**
     * [수정] 프로필 이미지를 업데이트하는 API
     *
     * @param principal 현재 로그인된 사용자 정보를 담고 있는 객체
     * @param imageFile 사용자가 업로드한 이미지 파일
     * @return 성공 시 새로운 이미지 URL을 담은 응답
     * @throws IOException 파일 처리 중 예외 발생 가능
     */
    @PostMapping("/api/profile/image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateProfileImage(Principal principal, @RequestParam("profileImage") MultipartFile imageFile) throws IOException {
        if (principal == null) {
            // 인증되지 않은 사용자의 요청은 거부
            return ResponseEntity.status(401).build(); // 401 Unauthorized
        }

        String userEmail = principal.getName();
        String newImageUrl = userInfoService.updateProfileImage(userEmail, imageFile);

        return ResponseEntity.ok(Collections.singletonMap("imageUrl", newImageUrl));
    }
}
