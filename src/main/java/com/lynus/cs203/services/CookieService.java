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
        log.debug("Setting refresh token cookie");

        ResponseCookie cookie = createRefreshTokenCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.debug("Refresh token cookie set successfully");
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        log.debug("Creating secure refresh token cookie");

        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/auth/refresh")
                .maxAge(jwtConfig.getRefreshTokenExpiration())   // 7 days
                .secure(true)
                .build();
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        log.debug("Clearing refresh token cookie");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Refresh token cookie cleared successfully");
    }
}
