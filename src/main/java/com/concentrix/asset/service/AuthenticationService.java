package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.ChangePasswordRequest;
import com.concentrix.asset.dto.request.LoginRequest;
import com.concentrix.asset.dto.response.LoginResponse;
import com.concentrix.asset.dto.response.RefreshResponse;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.text.ParseException;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request, HttpServletResponse httpServletResponse);

    void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ParseException, JOSEException;

    RefreshResponse refreshToken(HttpServletRequest request, HttpServletResponse response)
            throws ParseException, JOSEException;

    void changePassword(ChangePasswordRequest request);

}

