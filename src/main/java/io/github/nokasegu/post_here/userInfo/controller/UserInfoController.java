package io.github.nokasegu.post_here.userInfo.controller;

import io.github.nokasegu.post_here.userInfo.dto.NameUpdateDto;
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
        log.info("로그인 페이지 호출");
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
    @GetMapping("/check-email")
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
    @GetMapping("/check-nickname")
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
    @PostMapping("/send-verification")
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
    @PostMapping("/verify-code")
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
//    @GetMapping("/profile")
//    public String profilePage(@AuthenticationPrincipal UserDetails user, Model model) {
//
//        String userEmail = user.getUsername();
//        UserInfoDto userProfile = userInfoService.getUserProfileByEmail(userEmail);
//
//        model.addAttribute("user", userProfile);
//        return "userInfo/profile";
//    }
//
//    /**
//     * 다른 사람의 프로필 페이지(닉네임 기반)
//     * - 템플릿은 동일하게 userInfo/profile 사용
//     * - 뷰에서 profileUserNickname 유무로 내/타인 분기 가능
//     */
//    @GetMapping("/profile/{nickname}")
//    public String otherProfilePage(@PathVariable String nickname, Model model) {
//        model.addAttribute("profileUserNickname", nickname);
//        // 필요시: userInfoService.getUserProfileByNickname(nickname)으로 미리 바인딩
//        return "userInfo/profile";
//    }

    /**
     * [수정] 프로필 이미지를 업데이트하는 API
     *
     * @param principal 현재 로그인된 사용자 정보를 담고 있는 객체
     * @param imageFile 사용자가 업로드한 이미지 파일
     * @return 성공 시 새로운 이미지 URL을 담은 응답
     * @throws IOException 파일 처리 중 예외 발생 가능
     */
    @PostMapping("/profile/image")
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

    /**
     * [수정] 내 프로필 페이지를 직접 보여줍니다. (리다이렉트 없음)
     * URL: /profile
     */
    @GetMapping("/profile")
    public String myProfilePage(@AuthenticationPrincipal UserDetails user, Model model) {
        // 1. 로그인하지 않은 사용자는 로그인 페이지로 보냅니다.
        if (user == null) {
            return "redirect:/login";
        }

        // 2. 현재 로그인된 사용자의 이메일로 프로필 정보를 조회합니다.
        String userEmail = user.getUsername();
        UserInfoDto userProfile = userInfoService.getUserProfileByEmail(userEmail);

        // 3. 모델에 'profileUser'라는 이름으로 사용자 정보를 담습니다.
        model.addAttribute("profileUser", userProfile);
        // 4. 이 페이지가 '내 프로필'임을 명확히 알려주는 플래그를 추가합니다.
        model.addAttribute("isMyProfile", true);

        // 5. 프로필 템플릿을 직접 렌더링합니다.
        return "userInfo/profile";
    }

    /**
     * [수정] 닉네임 기반으로 다른 사용자의 프로필 페이지를 보여줍니다.
     * URL: /profile/{nickname}
     */
    @GetMapping("/profile/{nickname}")
    public String userProfilePage(@PathVariable String nickname, @AuthenticationPrincipal UserDetails currentUser, Model model) {

        // 1. URL로 들어온 닉네임의 프로필 정보를 조회합니다.
        UserInfoDto profileUser = userInfoService.getUserProfileByNickname(nickname, currentUser);

        // 2. 현재 로그인한 사용자인지 여부를 확인합니다.
        boolean isMyProfile = false;
        if (currentUser != null) {
            // 현재 로그인한 사용자의 닉네임과 URL의 닉네임이 같은지 비교합니다.
            // 참고: currentUser.getUsername()은 이메일이므로, DB 조회가 한 번 더 필요합니다.
            UserInfoDto myInfo = userInfoService.getUserProfileByEmail(currentUser.getUsername());
            if (myInfo.getNickname().equals(nickname)) {
                // 만약 같다면, 내 프로필 URL(/profile)로 보내버리는 것이 더 사용자 경험에 좋습니다.
                return "redirect:/profile";
            }
        }

        // 3. 모델에 데이터 추가
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("isMyProfile", isMyProfile); // 이 경우 항상 false가 됩니다.

        return "userInfo/profile";
    }

    @PostMapping("/profile/edit/nickname")
    public ResponseEntity<Void> updateNickname(Principal principal, @RequestBody NameUpdateDto requestDto) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 401 Unauthorized
        }

        userInfoService.updateNickname(principal.getName(), requestDto.getNickname());

        return ResponseEntity.ok().build();
    }


}