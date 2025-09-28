package com.lynus.cs203.services;

import com.lynus.cs203.config.JwtConfig;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CookieService {
    private final JwtConfig jwtConfig;

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        log.info("Setting refresh token cookie for user");

        ResponseCookie cookie = createRefreshTokenCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        log.info("Creating refresh token for user: {}", refreshToken);

        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/auth/refresh")
                .maxAge(jwtConfig.getRefreshTokenExpiration())   // 7 days
                .secure(true)
                .build();
    }

}
