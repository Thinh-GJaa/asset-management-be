package com.concentrix.asset.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

        OPTIMISTIC_LOCK(10002, HttpStatus.CONFLICT, "OptimisticLock occurred"),

        SERIAL_NUMBER_ALREADY_EXISTS(10003, HttpStatus.CONFLICT, "Serial number '{}' already exists"),

        STOCK_OUT(10005, HttpStatus.CONFLICT, "Stock out of '{}'"),
        DEVICE_NOT_FOUND_IN_WAREHOUSE(10004, HttpStatus.NOT_FOUND, "Device '{}' not found in warehouse '{}'"),
        // Authentication and authorization errors
        PASSWORD_INCORRECT(10400, HttpStatus.UNAUTHORIZED, "Password incorrect"),
        CONFIRM_PASSWORD_NOT_MATCH(10401, HttpStatus.BAD_REQUEST, "Confirm password does not match"),
        CURRENT_PASSWORD_INCORRECT(10402, HttpStatus.BAD_REQUEST, "Current password incorrect"),
        UNAUTHORIZED(10403, HttpStatus.FORBIDDEN, "You do not have permission"),
        UNAUTHENTICATED(10404, HttpStatus.UNAUTHORIZED, "Unauthenticated"),
        ACCESS_DENIED(10405, HttpStatus.FORBIDDEN, "Access denied"),
        EMAIL_NOT_FOUND(10406, HttpStatus.NOT_FOUND, "Email not found"),
        INVALID_PASSWORD(10407, HttpStatus.UNAUTHORIZED, "Invalid password"),
        LOGIN_FORBIDDEN(10408, HttpStatus.FORBIDDEN, "You do not have permission to login"),

        // Role-related errors
        ROLE_NOT_FOUND(10600, HttpStatus.NOT_FOUND, "Role with name '{}' not found"),

        COOKIE_NOT_FOUND(12000, HttpStatus.UNAUTHORIZED, "Cookie '{}' not found"),

        // Database-related errors
        DATABASE_CONNECTION_FAILED(10700, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect to the database"),
        DATA_INTEGRITY_VIOLATION(10701, HttpStatus.CONFLICT, "Data integrity violation"),
        RECORD_ALREADY_EXISTS(10702, HttpStatus.CONFLICT, "Record already exists"),
        RECORD_NOT_FOUND(10703, HttpStatus.NOT_FOUND, "Record not found"),

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
        RESET_PASSWORD_TOKEN_INCORRECT(12301, HttpStatus.UNAUTHORIZED, "Reset password token incorrect"),

        // Site entity errors
        SITE_NOT_FOUND(20001, HttpStatus.NOT_FOUND, "Site with ID '{}' not found"),
        SITE_ALREADY_EXISTS(20002, HttpStatus.CONFLICT, "Site with name '{}' already exists"),
        SITE_UPDATE_FAILED(20003, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update site with ID '{}'"),
        SITE_DELETE_FAILED(20004, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete site with ID '{}'"),

        // User entity errors
        USER_NOT_FOUND(20101, HttpStatus.NOT_FOUND, "User with ID '{}' not found"),
        USER_ALREADY_EXISTS(20102, HttpStatus.CONFLICT, "User with username '{}' already exists"),
        USER_UPDATE_FAILED(20103, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update user with ID '{}'"),
        USER_DELETE_FAILED(20104, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete user with ID '{}'"),
        SSO_ALREADY_EXISTS(20105, HttpStatus.CONFLICT, "User with SSO '{}' already exists"),
        MSA_ALREADY_EXISTS(20106, HttpStatus.CONFLICT, "User with MSA '{}' already exists"),
        EMAIL_ALREADY_EXISTS(20107, HttpStatus.CONFLICT, "User with email '{}' already exists"),

        // Device entity errors
        DEVICE_NOT_FOUND(20201, HttpStatus.NOT_FOUND, "Device '{}' not found"),
        DEVICE_ALREADY_EXISTS(20202, HttpStatus.CONFLICT, "Device with serial number '{}' already exists"),
        DEVICE_UPDATE_FAILED(20203, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update device with ID '{}'"),
        DEVICE_DELETE_FAILED(20204, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete device with ID '{}'"),
        INVALID_DEVICE_STATUS(20205, HttpStatus.BAD_REQUEST,
                        "Device '{}' is not in valid status"),
        INVALID_DEVICE_USER(20206, HttpStatus.BAD_REQUEST, "Device '{}' is not assigned to this user"),
        RETURN_QUANTITY_EXCEEDS_BORROWED(20207, HttpStatus.BAD_REQUEST,
                        "Return quantity exceeds borrowed amount for device '{}'"),

        SEAT_NUMBER_ALREADY_EXISTS(20208, HttpStatus.CONFLICT, "Seat number '{}' already exists"),

        // Stock entity errors
        STOCK_NOT_FOUND(20301, HttpStatus.NOT_FOUND, "Stock with ID '{}' not found"),
        STOCK_ALREADY_EXISTS(20302, HttpStatus.CONFLICT, "Stock with name '{}' already exists"),
        STOCK_UPDATE_FAILED(20303, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update stock with ID '{}'"),
        STOCK_DELETE_FAILED(20304, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete stock with ID '{}'"),

        // Account entity errors
        ACCOUNT_NOT_FOUND(20401, HttpStatus.NOT_FOUND, "Account with ID '{}' not found"),
        ACCOUNT_ALREADY_EXISTS(20402, HttpStatus.CONFLICT, "Account with username '{}' already exists"),
        ACCOUNT_NAME_ALREADY_EXISTS(20403, HttpStatus.CONFLICT, "Account with name '{}' already exists"),
        ACCOUNT_CODE_ALREADY_EXISTS(20404, HttpStatus.CONFLICT, "Account with code '{}' already exists"),
        ACCOUNT_NAME_NOT_FOUND(20405, HttpStatus.NOT_FOUND, "Account with name '{}' not found"),


        // DeviceType entity errors
        DEVICETYPE_NOT_FOUND(20501, HttpStatus.NOT_FOUND, "DeviceType with ID '{}' not found"),
        DEVICETYPE_ALREADY_EXISTS(20502, HttpStatus.CONFLICT, "DeviceType with name '{}' already exists"),
        DEVICETYPE_UPDATE_FAILED(20503, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update device type with ID '{}'"),
        DEVICETYPE_DELETE_FAILED(20504, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete device type with ID '{}'"),

        // DeviceModel entity errors
        DEVICEMODEL_NOT_FOUND(20601, HttpStatus.NOT_FOUND, "DeviceModel with ID '{}' not found"),
        DEVICEMODEL_ALREADY_EXISTS(20602, HttpStatus.CONFLICT, "DeviceModel with name '{}' already exists"),
        DEVICEMODEL_UPDATE_FAILED(20603, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to update device model with ID '{}'"),
        DEVICEMODEL_DELETE_FAILED(20604, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete device model with ID '{}'"),

        // ExportTicket entity errors
        EXPORTTICKET_NOT_FOUND(20701, HttpStatus.NOT_FOUND, "ExportTicket with ID '{}' not found"),
        EXPORTTICKET_ALREADY_EXISTS(20702, HttpStatus.CONFLICT, "ExportTicket with code '{}' already exists"),
        EXPORTTICKET_UPDATE_FAILED(20703, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to update export ticket with ID '{}'"),
        EXPORTTICKET_DELETE_FAILED(20704, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete export ticket with ID '{}'"),

        // MaintenanceTicket entity errors
        MAINTENANCETICKET_NOT_FOUND(20801, HttpStatus.NOT_FOUND, "MaintenanceTicket with ID '{}' not found"),
        MAINTENANCETICKET_ALREADY_EXISTS(20802, HttpStatus.CONFLICT, "MaintenanceTicket with code '{}' already exists"),
        MAINTENANCETICKET_UPDATE_FAILED(20803, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to update maintenance ticket with ID '{}'"),
        MAINTENANCETICKET_DELETE_FAILED(20804, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete maintenance ticket with ID '{}'"),

        // DisposalTicket entity errors
        DISPOSALTICKET_NOT_FOUND(20901, HttpStatus.NOT_FOUND, "DisposalTicket with ID '{}' not found"),
        DISPOSALTICKET_ALREADY_EXISTS(20902, HttpStatus.CONFLICT, "DisposalTicket with code '{}' already exists"),
        DISPOSALTICKET_UPDATE_FAILED(20903, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to update disposal ticket with ID '{}'"),
        DISPOSALTICKET_DELETE_FAILED(20904, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete disposal ticket with ID '{}'"),

        // Assignment entity errors
        ASSIGNMENT_NOT_FOUND(21001, HttpStatus.NOT_FOUND, "Assignment with ID '{}' not found"),
        ASSIGNMENT_ALREADY_EXISTS(21002, HttpStatus.CONFLICT, "Assignment with code '{}' already exists"),
        ASSIGNMENT_UPDATE_FAILED(21003, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update assignment with ID '{}'"),
        ASSIGNMENT_DELETE_FAILED(21004, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete assignment with ID '{}'"),

        // Vendor entity errors
        VENDOR_NOT_FOUND(21301, HttpStatus.NOT_FOUND, "Vendor with ID '{}' not found"),
        VENDOR_ALREADY_EXISTS(21302, HttpStatus.CONFLICT, "Vendor with name '{}' already exists"),
        VENDOR_UPDATE_FAILED(21303, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update vendor with ID '{}'"),
        VENDOR_DELETE_FAILED(21304, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete vendor with ID '{}'"),

        // Transaction entity errors
        TRANSACTION_NOT_FOUND(21101, HttpStatus.NOT_FOUND, "Transaction with ID '{}' not found"),
        TRANSACTION_ALREADY_EXISTS(21102, HttpStatus.CONFLICT, "Transaction with code '{}' already exists"),
        TRANSACTION_UPDATE_FAILED(21103, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction with ID '{}'"),
        TRANSACTION_DELETE_FAILED(21104, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete transaction with ID '{}'"),
        TRANSACTION_TYPE_INVALID(21105, HttpStatus.BAD_REQUEST, "Transaction type is invalid"),
        TRANSACTION_STATUS_INVALID(21106, HttpStatus.BAD_REQUEST, "Transaction status is invalid"),
        DUPLICATE_DEVICE_RETURN(21107, HttpStatus.BAD_REQUEST, "Duplicate device in return list: '{}'"),

        // Warehouse entity errors
        WAREHOUSE_NOT_FOUND(21201, HttpStatus.NOT_FOUND, "Warehouse with ID '{}' not found"),
        WAREHOUSE_ALREADY_EXISTS(21202, HttpStatus.CONFLICT, "Warehouse with name '{}' already exists"),
        WAREHOUSE_UPDATE_FAILED(21203, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update warehouse with ID '{}'"),
        WAREHOUSE_DELETE_FAILED(21204, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete warehouse with ID '{}'"),

        // Model entity errors
        MODEL_NOT_FOUND(21401, HttpStatus.NOT_FOUND, "Model with ID '{}' not found"),
        MODEL_ALREADY_EXISTS(21402, HttpStatus.CONFLICT, "Model with name '{}' already exists"),
        MODEL_UPDATE_FAILED(21403, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update model with ID '{}'"),
        MODEL_DELETE_FAILED(21404, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete model with ID '{}'"),

        // Purchase Order entity errors
        PO_NOT_FOUND(21501, HttpStatus.NOT_FOUND, "Purchase Order with ID '{}' not found"),
        PO_ALREADY_EXISTS(21502, HttpStatus.CONFLICT, "Purchase Order with ID '{}' already exists"),

        DUPLICATE_SERIAL_NUMBER(21601, HttpStatus.CONFLICT, "Duplicate serial number '{}'"),

        // New errors
        NEW_ERROR(21400, HttpStatus.INTERNAL_SERVER_ERROR, "New error occurred"),

        // Date validation errors
        INVALID_DATE_RANGE(21410, HttpStatus.BAD_REQUEST, "fromDate must be before toDate"),

        // Repair entity errors
        REPAIR_NOT_FOUND(22001, HttpStatus.NOT_FOUND, "Repair with ID '{}' not found"),
        REPAIR_ALREADY_EXISTS(22002, HttpStatus.CONFLICT, "Repair with code '{}' already exists"),
        REPAIR_UPDATE_FAILED(22003, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update repair with ID '{}'"),
        REPAIR_DELETE_FAILED(22004, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete repair with ID '{}'"),

        // Floor entity errors
        FLOOR_NOT_FOUND(21701, HttpStatus.NOT_FOUND, "Floor with ID '{}' not found"),
        FLOOR_ALREADY_EXISTS(21702, HttpStatus.CONFLICT, "Floor with name '{}' already exists"),
        FLOOR_UPDATE_FAILED(21703, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update floor with ID '{}'"),
        FLOOR_DELETE_FAILED(21704, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete floor with ID '{}'"),

        INVALID_FLOOR_TRANSFER(21700, HttpStatus.BAD_REQUEST, "Invalid floor transfer: From Floor and To Floor must be different and belong to the same site"),
        INVALID_SITE_TRANSFER(21706, HttpStatus.BAD_REQUEST, "Invalid site transfer: The two warehouses belong to different sites"),
        INVALID_USE_FLOOR(21707, HttpStatus.BAD_REQUEST, "Invalid use floor: Floor and Warehouse must belong to the same site"),
        // Custom: Device not found in floor
        INVALID_RETURN_DATE( 21708, HttpStatus.BAD_REQUEST, "Return date must be after the current date"),

        DEVICE_NOT_FOUND_IN_FLOOR(21705, HttpStatus.NOT_FOUND, "Device '%s' not found in floor '%s'"),
        DEVICE_NOT_ENOUGH_IN_FLOOR(21709, HttpStatus.BAD_REQUEST, "Device '%s' not enough in floor '%s'"),
        DEVICE_NOT_FOUND_IN_STOCK(21710, HttpStatus.BAD_REQUEST, "Device '%s' not enough in stock '%s'");

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
