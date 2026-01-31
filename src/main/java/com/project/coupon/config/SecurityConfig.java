package com.project.coupon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정.
     *
     * - JSESSIONID 기반 세션(기본 값) 사용
     * - /login 은 로그인 페이지로 허용
     * - 로그인 성공 시 /events 로 이동
     * - 그 외 요청은 ROLE_USER 필요
     * - CORS 설정은 CorsConfig 의 Bean 을 사용
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // CSRF 는 우선 비활성화 (필요 시 쿠폰 발급 API 특성에 맞게 조정)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 - CorsConfig 에서 정의한 Bean 사용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // 세션 정책: 필요할 때 생성 (JSESSIONID 기반)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // URL 접근 제어
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().hasRole("USER")
                )

                // 폼 로그인 설정
                .formLogin(form -> form
                        .loginPage("/login")              // 로그인 페이지 URL
                        .loginProcessingUrl("/login")     // 인증 처리 URL (form action)
                        .defaultSuccessUrl("/events", true)     // 로그인 성공 시 /events 로 이동
                        .permitAll()
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
