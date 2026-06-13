package com.esun.seatsystem.common.config;

import com.esun.seatsystem.common.dto.ApiResponse;
import com.esun.seatsystem.common.security.JwtCookieFilter;
import com.esun.seatsystem.common.security.JwtService;
import com.esun.seatsystem.common.security.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * 共用層：Spring Security 設定。
 * 無狀態（JWT in httpOnly cookie）、依角色授權、CSRF 由 SameSite=Strict cookie 防護。
 */
@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public RateLimiter loginRateLimiter(
            @Value("${app.security.login-max-attempts}") int maxAttempts,
            @Value("${app.security.login-window-minutes}") int windowMinutes) {
        return new RateLimiter(maxAttempts, Duration.ofMinutes(windowMinutes));
    }

    @Bean
    public RateLimiter resetRateLimiter(
            @Value("${app.reset-code.max-attempts}") int maxAttempts,
            @Value("${app.reset-code.expiration-minutes}") int windowMinutes) {
        return new RateLimiter(maxAttempts, Duration.ofMinutes(windowMinutes));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 無狀態 + SameSite=Strict cookie，不需 CSRF token
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 靜態資源與登入/忘記密碼端點公開
                        .requestMatchers("/", "/login", "/forgot-password",
                                "/index.html", "/assets/**", "/favicon.ico", "/vite.svg").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        // 異動公告 / 座位需 ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/announcements").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/seats/batch").hasRole("ADMIN")
                        // 其餘 API 需登入
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "A401", "請先登入"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, HttpServletResponse.SC_FORBIDDEN, "A403", "權限不足")))
                .addFilterBefore(new JwtCookieFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 開發期允許 Vite dev server 帶 cookie 呼叫；正式環境同源不需要 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void writeError(HttpServletResponse res, int status, String code, String message) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ApiResponse.error(code, message));
    }
}
