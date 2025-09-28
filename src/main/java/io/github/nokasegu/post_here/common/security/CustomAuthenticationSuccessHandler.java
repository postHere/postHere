package io.github.nokasegu.post_here.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.userInfo.dto.CheckLoginDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        response.setHeader("Location", "/forumMain");

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        WrapperDTO<CheckLoginDto> check = WrapperDTO.<CheckLoginDto>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(new CheckLoginDto(user.getUsername(), true))
                .build();

        objectMapper.writeValue(response.getWriter(), check);
    }
}
