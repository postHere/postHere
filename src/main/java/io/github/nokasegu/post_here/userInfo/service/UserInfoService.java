package io.github.nokasegu.post_here.userInfo.service;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.dto.UserInfoDto;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
//import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    private final PasswordEncoder passwordEncoder;

    private final S3UploaderService s3UploaderService;

    /**
     * 회원가입 로직
     *
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호
     * @param nickname 사용자 닉네임
     */
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
    public boolean isEmailAvailable(String email) {
        return userInfoRepository.findByEmail(email).isEmpty();
    }

    /**
     * 닉네임 중복 체크
     *
     * @param nickname 확인할 닉네임
     * @return 중복이면 false, 미중복이면 true
     */
    public boolean isNicknameAvailable(String nickname) {
        return userInfoRepository.findByNickname(nickname).isEmpty();
    }

    /**
     * [추가] 이메일로 사용자 프로필 정보를 조회하여 DTO로 반환합니다.
     *
     * @param email Principal.getName()으로 얻은 현재 로그인된 사용자의 이메일
     * @return 사용자 프로필 정보를 담은 DTO
     */
    public UserInfoDto getUserProfileByEmail(String email) {
        UserInfoEntity user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return new UserInfoDto(user);
    }

    /**
     * PK로 사용자 조회하는 메소드
     *
     * @param userId
     * @return 사용자 프로필 정보를 담은 DTO
     */
    public UserInfoDto getUserProfileById(Long userId) {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id : " + userId));
        return new UserInfoDto(user);
    }

    /**
     * [수정] 프로필 이미지를 업데이트하는 서비스 로직
     *
     * @param email     Principal.getName()으로 얻은 현재 로그인된 사용자의 이메일
     * @param imageFile 새로 업로드된 이미지 파일
     * @return S3에 업로드된 새로운 이미지의 URL
     * @throws IOException 파일 처리 중 예외 발생 가능
     */
    public String updateProfileImage(String email, MultipartFile imageFile) throws IOException {
        UserInfoEntity user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // 기존 프로필 사진 URL 가져오기
        String oldImageUrl = user.getProfilePhotoUrl();

        // S3에 새 이미지 업로드
        String newImageUrl = s3UploaderService.upload(imageFile, "profile-images");

        // 데이터베이스에 새 URL 저장
        user.setProfilePhotoUrl(newImageUrl);

        // 기존 이미지가 있고, 기본 이미지가 아닐 경우 S3에서 삭제
        if (oldImageUrl != null && !oldImageUrl.equals(s3UploaderService.getDefaultProfileImage())) {
            s3UploaderService.delete(oldImageUrl);
        }

        return newImageUrl;
    }


}
