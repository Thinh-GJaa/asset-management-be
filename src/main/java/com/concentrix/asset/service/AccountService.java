package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateAccountRequest;
import com.concentrix.asset.dto.request.UpdateAccountRequest;
import com.concentrix.asset.dto.response.AccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    AccountResponse getAccountById(Integer accountId);

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse updateAccount(UpdateAccountRequest request);

    Page<AccountResponse> filterAccount(String search, Pageable pageable);

}