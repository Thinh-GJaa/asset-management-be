package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.LoginRequest;
import com.concentrix.asset.dto.response.LoginResponse;
import com.concentrix.asset.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

        AuthenticationService authenticationService;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                @Valid @RequestBody LoginRequest loginRequest,
                HttpServletResponse httpServletResponse) {

                ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                        .message("Login successful")
                        .data(authenticationService.login(loginRequest, httpServletResponse))
                        .build();
                return ResponseEntity.ok(response);
        }

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest)
                                                                        throws ParseException, JOSEException {
                authenticationService.logout(httpServletRequest, httpServletResponse);
                ApiResponse<Void> response = ApiResponse.<Void>builder()
                        .message("Logout successful")
                        .build();
                return ResponseEntity.ok(response);
        }


}