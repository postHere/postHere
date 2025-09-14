package io.github.nokasegu.post_here.userInfo.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.userInfo.dto.CheckLoginDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/api/auth/status")
    public WrapperDTO<CheckLoginDto> checkAuthenticated() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        CheckLoginDto dto = new CheckLoginDto();

        // 1. 인증 정보가 존재하고,
        // 2. 사용자가 인증된 상태이며,
        // 3. 익명 사용자가 아닌 경우 ('anonymousUser'는 Spring Security가 비로그인 사용자에게 부여하는 이름)
        dto.setAuthenticated(auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString()));

        return WrapperDTO.<CheckLoginDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(dto)
                .build();
    }
}
