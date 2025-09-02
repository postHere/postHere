package io.github.nokasegu.post_here.userInfo.service;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 로직
     *
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호
     * @param nickname 사용자 닉네임
     */
    @Transactional
    public void signUp(String email, String password, String nickname) {
        // 이메일과 닉네임 중복 여부를 다시 한번 확인하는 것이 안전합니다.
        if (!isEmailAvailable(email)) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (!isNicknameAvailable(nickname)) {
            throw new IllegalArgumentException("Nickname already exists.");
        }

        // 비밀번호 암호화
        // String encodedPassword = passwordEncoder.encode(password);

        UserInfoEntity user = UserInfoEntity.builder()
                .email(email)
                .loginPw(passwordEncoder.encode(password)) // 암호화된 비밀번호를 저장해야 합니다.
                .nickname(nickname)
                .build();

        userInfoRepository.save(user);
    }

    /**
     * * 이메일 중복 체크
     *
     * @param email 확인할 이메일
     * @return 중복이면 false, 미중복이면 true
     */

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return userInfoRepository.findByEmail(email).isEmpty();
    }

    /**
     * 닉네임 중복 체크
     *
     * @param nickname 확인할 닉네임
     * @return 중복이면 false, 미중복이면 true
     */
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return userInfoRepository.findByNickname(nickname).isEmpty();
    }


}
