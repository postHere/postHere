package io.github.nokasegu.post_here.userInfo.controller;

import io.github.nokasegu.post_here.userInfo.service.EmailService;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
        userInfoService.signUp(email, password, nickname);
        log.debug("사용자 정보{}, {}, {}", email, password, nickname);
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

    /*
     * 프로필 창으로 이동
     */
    @GetMapping("/profile")
    public String profilePage() {
        return "userInfo/profile";
    }

    /*
    프로필 이미지 수정 기능
     */
    @PostMapping("/profile")
    public String postProfilePage() {
        return "userInfo/profile";
    }
}
