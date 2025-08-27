package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateAccountRequest;
import com.concentrix.asset.dto.request.UpdateAccountRequest;
import com.concentrix.asset.dto.response.AccountResponse;
import com.concentrix.asset.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {

    AccountResponse getAccountById(Integer accountId);

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse updateAccount(UpdateAccountRequest request);

    Page<AccountResponse> filterAccount(String search, Pageable pageable);

    List<UserResponse> getOwners();

}