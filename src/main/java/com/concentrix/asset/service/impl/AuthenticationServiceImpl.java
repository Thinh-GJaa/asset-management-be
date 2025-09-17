package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.ChangePasswordRequest;
import com.concentrix.asset.dto.request.LoginRequest;
import com.concentrix.asset.dto.response.LoginResponse;
import com.concentrix.asset.dto.response.RefreshResponse;
import com.concentrix.asset.entity.InvalidatedToken;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.repository.InvalidatedTokenRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.AuthenticationService;
import com.concentrix.asset.service.UserService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserService userService;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${jwt.issuer}")
    protected String ISSUER;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("[AUTHENTICATION SERVICE] Invalid password for user: {}", request.getEmail());
            throw new CustomException(ErrorCode.PASSWORD_INCORRECT);
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        setRefreshTokenInHttpOnlyCookie(refreshToken, response);
        log.info("[AUTHENTICATION SERVICE] User {} logged in successfully", user.getEmail());
        return LoginResponse.builder()
                .token(accessToken)
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) throws ParseException, JOSEException {
        // Lấy refresh token từ cookie
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("[AUTHENTICATION SERVICE][TOKEN] Refresh token is missing from cookie");
            clearRefreshTokenFromCookie(response); // Xóa cookie nếu có
            return;
        }

        try {
            SignedJWT signedJWT = verifyToken(refreshToken, true);

            String jit = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            invalidateToken(jit, expiryTime);

            log.info("[AUTHENTICATION SERVICE] Logout successful for user: {}",
                    signedJWT.getJWTClaimsSet().getSubject());
            clearRefreshTokenFromCookie(response);
        } catch (CustomException e) {
            log.error("[AUTHENTICATION SERVICE] Failed to logout: {}", e.getMessage(), e);
            throw e; // Ném lại lỗi để client biết lý do logout thất bại
        } finally {
            clearRefreshTokenFromCookie(response); // Luôn xóa cookie ở client
        }
    }

    @Override
    public RefreshResponse refreshToken(HttpServletRequest request, HttpServletResponse response)
            throws ParseException, JOSEException {
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("[AUTHENTICATION SERVICE] Refresh token not found in cookie");
            throw new CustomException(ErrorCode.COOKIE_NOT_FOUND, "refresh_token");
        }

        try {
            // Xác minh Refresh Token
            SignedJWT signedJWT = verifyToken(refreshToken, true);
            String tokenId = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            invalidateToken(tokenId, expiryTime);

            String username = signedJWT.getJWTClaimsSet().getSubject();
            User user = userRepository.findById(username)
                    .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));

            String newAccessToken = generateAccessToken(user);
            String newRefreshToken = generateRefreshToken(user);

            setRefreshTokenInHttpOnlyCookie(newRefreshToken, response);

            log.info("[AUTHENTICATION SERVICE] Token refreshed successfully for user: {}", username);
            return RefreshResponse.builder().accessToken(newAccessToken).build();
        } catch (CustomException e) {
            log.error("[AUTHENTICATION SERVICE] Failed to refresh token: {}", e.getMessage(), e);
            throw e;
        }
    }



    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = userService.getCurrentUser();

        if(!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new CustomException(ErrorCode.CONFIRM_PASSWORD_NOT_MATCH);

        if(!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new CustomException(ErrorCode.CURRENT_PASSWORD_INCORRECT);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }






    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setRefreshTokenInHttpOnlyCookie(String refreshToken, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) REFRESHABLE_DURATION);

        response.addCookie(refreshTokenCookie);
    }

    // Phương thức xóa Refresh Token khỏi cookie
    private void clearRefreshTokenFromCookie(HttpServletResponse response) {
        // Tạo một cookie mới với tên "refresh_token" nhưng không có giá trị và đặt thời
        // gian sống bằng 0
        Cookie refreshTokenCookie = new Cookie("refresh_token", null);
        refreshTokenCookie.setHttpOnly(true); // Đảm bảo cookie không thể truy cập qua JavaScript
        refreshTokenCookie.setSecure(true); // Chỉ gửi cookie qua HTTPS
        refreshTokenCookie.setPath("/"); // Cookie có thể được truy cập từ tất cả các endpoint
        refreshTokenCookie.setMaxAge(0); // Đặt thời gian sống cookie bằng 0 để xóa nó

        // Thêm cookie vào response để xóa nó ở client
        response.addCookie(refreshTokenCookie);
        log.info("[AUTHENTICATION SERVICE] Refresh token cookie cleared");
    }

    private void invalidateToken(String tokenId, Date expiryTime) {
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(tokenId)
                .expiryTime(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
        log.info("[AUTHENTICATION SERVICE] Token invalidated: {}", tokenId);
    }

    private String generateAccessToken(User user) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
            String tokenId = UUID.randomUUID().toString();
            Date expiryTime = new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getEid())
                    .issuer(ISSUER)
                    .issueTime(new Date())
                    .expirationTime(expiryTime)
                    .jwtID(tokenId)
                    .claim("role", "ROLE_" + user.getRole()) // Lưu role duy nhất, đúng chuẩn
                    .build();

            JWSObject jwsObject = new JWSObject(header, new Payload(claimsSet.toJSONObject()));
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));

            return "Bearer " + jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("[AUTHENTICATION SERVICE] [TOKEN] Failed to generate access token: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JWT_SIGN_ERROR);
        }
    }

    private String generateRefreshToken(User user) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
            String tokenId = UUID.randomUUID().toString();
            Date expiryTime = new Date(Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getEid())
                    .issuer(ISSUER)
                    .issueTime(new Date())
                    .expirationTime(expiryTime)
                    .jwtID(tokenId)
                    .claim("role", "ROLE_" + user.getRole())
                    .build();

            JWSObject jwsObject = new JWSObject(header, new Payload(claimsSet.toJSONObject()));
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));

            log.info("[AUTHENTICATION SERVICE][TOKEN] Refresh token generated for user: {}", user.getEid());

            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("[AUTHENTICATION SERVICE] Failed to generate refresh token: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JWT_SIGN_ERROR, "Refresh token generation failed");
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        try {
            token = token.replace("Bearer ", "");
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            if (!signedJWT.verify(verifier)) {
                log.warn("[AUTHENTICATION SERVICE] Invalid token signature");
                throw new CustomException(ErrorCode.TOKEN_SIGNATURE_INVALID);
            }

            if (expiryTime.before(new Date())) {
                log.warn("[AUTHENTICATION SERVICE] Token expired: {}", signedJWT.getJWTClaimsSet().getJWTID());
                throw new CustomException(ErrorCode.TOKEN_EXPIRED);
            }

            if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
                log.warn("[AUTHENTICATION SERVICE] Token already invalidated: {}",
                        signedJWT.getJWTClaimsSet().getJWTID());
                throw new CustomException(ErrorCode.TOKEN_ALREADY_INVALIDATED);
            }

            return signedJWT;
        } catch (ParseException e) {
            log.error("[AUTHENTICATION SERVICE] [JWT] JWT parse error: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JWT_PARSE_ERROR);
        } catch (JOSEException e) {
            log.error("[AUTHENTICATION SERVICE] [JWT] JWT verification error: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JWT_VERIFY_ERROR);
        }
    }

}
