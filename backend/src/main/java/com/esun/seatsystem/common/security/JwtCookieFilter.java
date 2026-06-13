package com.esun.seatsystem.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 共用層：從 httpOnly cookie 取出 JWT，驗證後寫入 SecurityContext。
 * token 無效（過期、竄改、格式錯誤）則視為未認證，交由授權規則處理。
 */
public class JwtCookieFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "ACCESS_TOKEN";

    private final JwtService jwtService;

    public JwtCookieFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtService.TokenInfo info = jwtService.parse(token);
                var authority = new SimpleGrantedAuthority("ROLE_" + info.role());
                var authentication = new UsernamePasswordAuthenticationToken(
                        info.empId(), null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ex) {
                // 無效 token：保持未認證狀態
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
