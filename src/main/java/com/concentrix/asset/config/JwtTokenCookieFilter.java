package com.concentrix.asset.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtTokenCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if ((authHeader == null || !authHeader.startsWith("Bearer "))
                && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    String token = cookie.getValue();

                    // Tạo một wrapper để thêm header Authorization
                    HttpServletRequest wrappedRequest =
                            new HttpServletRequestWrapperWithAuth(request, "Bearer " + token);

                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }
            }

        }

        filterChain.doFilter(request, response);
    }
}
