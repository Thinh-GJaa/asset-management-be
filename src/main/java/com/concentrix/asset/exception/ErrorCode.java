package com.concentrix.asset.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    OPTIMISTIC_LOCK(10002, HttpStatus.CONFLICT, "OptimisticLock occurred"),


    // Authentication and authorization errors
    PASSWORD_INCORRECT(10400, HttpStatus.UNAUTHORIZED, "Password incorrect"),
    CONFIRM_PASSWORD_NOT_MATCH(10401, HttpStatus.BAD_REQUEST, "Confirm password does not match"),
    CURRENT_PASSWORD_INCORRECT(10402, HttpStatus.BAD_REQUEST, "Current password incorrect"),
    UNAUTHORIZED(10403, HttpStatus.FORBIDDEN, "You do not have permission"),
    UNAUTHENTICATED(10404, HttpStatus.UNAUTHORIZED, "Unauthenticated"),
    ACCESS_DENIED(10405, HttpStatus.FORBIDDEN, "Access denied"),


    // Role-related errors
    ROLE_NOT_FOUND(10600, HttpStatus.NOT_FOUND, "Role with name '{}' not found"),

    // Database-related errors
    DATABASE_CONNECTION_FAILED(10700, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect to the database"),
    DATA_INTEGRITY_VIOLATION(10701, HttpStatus.CONFLICT, "Data integrity violation"),
    RECORD_ALREADY_EXISTS(10702, HttpStatus.CONFLICT, "Record already exists"),
    RECORD_NOT_FOUND(10703, HttpStatus.NOT_FOUND, "Record not found"),

   ENT_FAILED(11204, HttpStatus.PAYMENT_REQUIRED, "Payment for order with ID '{}' failed"),


    // Token-related errors
    TOKEN_NOT_FOUND(12100, HttpStatus.UNAUTHORIZED, "Token not found"),
    TOKEN_INVALID(12101, HttpStatus.UNAUTHORIZED, "Token is invalid"),
    TOKEN_EXPIRED(12102, HttpStatus.UNAUTHORIZED, "Token has expired"),
    TOKEN_SIGNATURE_INVALID(12103, HttpStatus.UNAUTHORIZED, "Token signature is invalid"),
    TOKEN_ALREADY_INVALIDATED(12104, HttpStatus.UNAUTHORIZED, "Token has already been invalidated"),
    TOKEN_TYPE_MISMATCH(12105, HttpStatus.BAD_REQUEST, "Token type mismatch"),
    TOKEN_MISSING_CLAIM(12106, HttpStatus.BAD_REQUEST, "Token missing required claim '{}'"),
    TOKEN_BLACKLISTED(12107, HttpStatus.UNAUTHORIZED, "Token is blacklisted"),

    // JWT-specific errors
    JWT_PARSE_ERROR(12200, HttpStatus.BAD_REQUEST, "Failed to parse JWT"),
    JWT_SIGN_ERROR(12201, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign JWT"),
    JWT_VERIFY_ERROR(12202, HttpStatus.UNAUTHORIZED, "Failed to verify JWT"),
    JWT_ISSUER_INVALID(12203, HttpStatus.UNAUTHORIZED, "JWT issuer is invalid"),
    JWT_AUDIENCE_INVALID(12204, HttpStatus.UNAUTHORIZED, "JWT audience is invalid"),
    JWT_CLAIM_INVALID(12205, HttpStatus.BAD_REQUEST, "JWT claim '{}' is invalid"),

    // Author-related errors
    AUTHOR_NOT_FOUND(11400, HttpStatus.NOT_FOUND, "Author with ID '{}' not found"),

    RESET_PASSWORD_TOKEN_NOT_EXISTS(12300, HttpStatus.NOT_FOUND, "Reset password token not exists or expired"),
    RESET_PASSWORD_TOKEN_INCORRECT(12301, HttpStatus.UNAUTHORIZED, "Reset password token incorrect");

    private final int code;
    private final HttpStatus status;
    private final String messageTemplate;

    ErrorCode(int code, HttpStatus status, String messageTemplate) {
        this.code = code;
        this.status = status;
        this.messageTemplate = messageTemplate;
    }

    public String formatMessage(Object... args) {
        return String.format(messageTemplate.replace("{}", "%s"), args);
    }
}
