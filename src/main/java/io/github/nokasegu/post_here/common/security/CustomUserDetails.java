package io.github.nokasegu.post_here.common.security;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final UserInfoEntity userInfo;

    @Override
    public String getUsername() {
        return userInfo.getLoginId();
    }

    @Override
    public String getPassword() {
        return userInfo.getLoginPw();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
