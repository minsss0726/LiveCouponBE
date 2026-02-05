package com.project.coupon.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
     * 세션에 SecurityContext 저장/로드용. AuthController JSON 로그인 시 명시 저장에 사용.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
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
                                                   @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // CSRF 는 우선 비활성화 (필요 시 쿠폰 발급 API 특성에 맞게 조정)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 - CorsConfig 에서 정의한 Bean 사용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // 세션 정책: 필요할 때 생성 (JSESSIONID 기반)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // 인증 실패 시 리다이렉트 대신 401 반환 (SPA에서 localhost:3000 리다이렉트 방지)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                // URL 접근 제어 (로그인 페이지는 React에서 제공)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/auth/login", "/login", "/login-form").permitAll()
                        .requestMatchers("/coupons/*/issue/*").permitAll()
                        .anyRequest().hasRole("USER")
                )

                // 폼 로그인: SPA는 /auth/login JSON API 사용. 리다이렉트 방지를 위해 별도 URL 사용
                .formLogin(form -> form
                        .loginPage("http://localhost:3000/login")
                        .loginProcessingUrl("/login-form")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
