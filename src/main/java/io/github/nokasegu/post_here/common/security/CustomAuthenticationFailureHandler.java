package io.github.nokasegu.post_here.common.security;

import io.github.nokasegu.post_here.common.exception.ErrorMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        String errorCode = ErrorMessage.LOGIN_FAIL.getCode();
        String errorMessage = ErrorMessage.LOGIN_FAIL.getValue();
        String location = "/login?code=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8)
                + "&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(location);
    }
}
