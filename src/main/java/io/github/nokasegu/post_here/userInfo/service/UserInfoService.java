package io.github.nokasegu.post_here.userInfo.service;

import io.github.nokasegu.post_here.common.util.S3UploaderService;
import io.github.nokasegu.post_here.follow.service.FollowingService;
import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.park.repository.ParkRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.dto.UserInfoDto;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
//import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3UploaderService s3UploaderService;
    private final FollowingService followingService;
    private final ParkRepository parkRepository;


    @Value("${custom.aws.s3.default-park}")
    private String DEFAULT_PARK;


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
        String defaultProfileImageUrl = s3UploaderService.getDefaultProfileImage();

        UserInfoEntity user = UserInfoEntity.builder()
                .email(email)
                .loginPw(passwordEncoder.encode(password)) // 암호화된 비밀번호를 저장해야 합니다.
                .nickname(nickname)
                .profilePhotoUrl(defaultProfileImageUrl)
                .build();

        userInfoRepository.save(user);

        ParkEntity park = ParkEntity.builder()
                .owner(user)
                .contentCaptureUrl(DEFAULT_PARK)
                .build();

        parkRepository.save(park);
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
     * 이메일로 사용자 프로필 정보를 조회하여 DTO로 반환합니다.
     *
     * @param email Principal.getName()으로 얻은 현재 로그인된 사용자의 이메일
     * @return 사용자 프로필 정보를 담은 DTO
     */
    public UserInfoDto getUserProfileByEmail(String email) {
        UserInfoEntity user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        long followerCount = followingService.getFollowerCount(user.getId());
        long followingCount = followingService.getFollowingCount(user.getId());

        String defaultProfileImageUrl = s3UploaderService.getDefaultProfileImage();

        return UserInfoDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profilePhotoUrl(user.getProfilePhotoUrl() != null
                        ? user.getProfilePhotoUrl()
                        : defaultProfileImageUrl)
                .followerCount(followerCount)   // 카운트 추가
                .followingCount(followingCount) // 카운트 추가
                .build();
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

    /**
     * 닉네임으로 사용자 프로필 정보를 조회하여 DTO로 반환합니다.
     *
     * @param nickname 조회할 사용자의 닉네임
     * @return 사용자 프로필 정보를 담은 DTO
     */
    public UserInfoDto getUserProfileByNickname(String nickname, UserDetails currentUser) {
        UserInfoEntity user = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("User not found with nickname: " + nickname));

        // FollowingService를 통해 팔로워/팔로잉 수 조회
        long followerCount = followingService.getFollowerCount(user.getId());
        long followingCount = followingService.getFollowingCount(user.getId());

        boolean isFollowing = false;
        if (currentUser != null) {
            UserInfoEntity loggedInUser = userInfoRepository.findByEmail(currentUser.getUsername()).orElse(null);
            if (loggedInUser != null) {
                // FollowingService의 getFollowingStatus를 활용하여 팔로우 상태 확인
                isFollowing = followingService.getFollowingStatus(loggedInUser.getId(), List.of(user.getId())).getOrDefault(user.getId(), false);
            }
        }

        String defaultProfileImageUrl = s3UploaderService.getDefaultProfileImage();

        // DTO를 빌더 패턴으로 생성하여 반환
        return UserInfoDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profilePhotoUrl(user.getProfilePhotoUrl() != null
                        ? user.getProfilePhotoUrl()
                        : defaultProfileImageUrl)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .build();
    }

    public UserInfoEntity getUserInfoByEmail(String email) {
        return userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public void updateNickname(String email, String newNickname) {
        // 1. 닉네임 중복 여부를 다시 한번 확인하여 안전성을 높입니다.
        if (!isNicknameAvailable(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 이메일로 사용자 정보를 찾아옵니다.
        UserInfoEntity user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // 3. 엔티티의 닉네임을 변경합니다.
        user.setNickname(newNickname);

        // 4. @Transactional 어노테이션에 의해 메소드가 끝나면 변경사항이 자동으로 DB에 저장됩니다.
    }

}
