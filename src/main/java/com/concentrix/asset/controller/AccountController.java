package com.concentrix.asset.controller;

import com.concentrix.asset.dto.ApiResponse;
import com.concentrix.asset.dto.request.CreateAccountRequest;
import com.concentrix.asset.dto.request.UpdateAccountRequest;
import com.concentrix.asset.dto.response.AccountResponse;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/account")
public class AccountController {

    AccountService accountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable Integer accountId) {

        ApiResponse<AccountResponse> apiResponse = ApiResponse.<AccountResponse>builder()
                .message("Get account ID successfully")
                .data(accountService.getAccountById(accountId))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {

        ApiResponse<AccountResponse> apiResponse = ApiResponse.<AccountResponse>builder()
                .message("Create account successfully")
                .data(accountService.createAccount(request))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(@Valid @RequestBody UpdateAccountRequest request) {

        ApiResponse<AccountResponse> apiResponse = ApiResponse.<AccountResponse>builder()
                .message("Update account successfully")
                .data(accountService.updateAccount(request))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> filterAccount(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, page = 0, sort = "accountName", direction = Sort.Direction.ASC) Pageable pageable) {
        ApiResponse<Page<AccountResponse>> apiResponse = ApiResponse.<Page<AccountResponse>>builder()
                .message("Get all accounts successfully")
                .data(accountService.filterAccount(search, pageable))
                .build();
        return ResponseEntity.ok(apiResponse);

    }

    @GetMapping("/owners")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getOwners() {

        ApiResponse<List<UserResponse>> apiResponse = ApiResponse.<List<UserResponse>>builder()
                .message("Get owners account successfully")
                .data(accountService.getOwners())
                .build();

        return ResponseEntity.ok(apiResponse);

    }

}