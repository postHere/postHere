package io.github.nokasegu.post_here.common.config;

import io.github.nokasegu.post_here.common.security.CustomAuthenticationFailureHandler;
import io.github.nokasegu.post_here.common.security.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final String[] WHITELIST_URL = {
            "/", "/signup/**", "/login",
            "/css/**", "/js/**", "/api/**",
            "/pwa/manifest.json", "/service-worker.js",

            // ✅ [추가] 푸시 VAPID 공개키는 비로그인 접근 허용이 필요
            //  - 프론트가 앱 시작 시 fetch로 키를 받아야 serviceWorker 구독 진행 가능
            //  - 공개키는 노출되어도 안전 (비공개키는 서버에만)
            "/push/vapid-public-key"
    };

    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 현재 프로젝트 기본 정책 유지 (CORS/CSRF 비활성화)
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);

        http
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .usernameParameter("id")
                        .passwordParameter("password")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                );

        http
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                );

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST_URL).permitAll()
                        // ✅ /push/subscribe 는 인증 필요 (사용자 계정과 구독 매핑 목적)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
