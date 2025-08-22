package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateAccountRequest;
import com.concentrix.asset.dto.request.UpdateAccountRequest;
import com.concentrix.asset.dto.response.AccountResponse;
import com.concentrix.asset.entity.Account;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AccountMapper;
import com.concentrix.asset.repository.AccountRepository;
import com.concentrix.asset.service.AccountService;
import com.concentrix.asset.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AccountServiceImpl implements AccountService {

    AccountRepository accountRepository;
    AccountMapper accountMapper;
    UserService userService;

    @Override
    public AccountResponse getAccountById(Integer accountId) {
        return accountMapper.toAccountResponse(
                accountRepository.findById(accountId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND, accountId))
        );
    }

    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {

        if(accountRepository.findByAccountName(request.getAccountName()) != null) {
            throw new CustomException(ErrorCode.ACCOUNT_NAME_ALREADY_EXISTS, request.getAccountName());
        }

        if(accountRepository.findByAccountCode(request.getAccountCode()) != null) {
            throw new CustomException(ErrorCode.ACCOUNT_CODE_ALREADY_EXISTS, request.getAccountCode());
        }

        Account account = accountMapper.toAccount(request);
        account.setCreatedBy(userService.getCurrentUser());

        return accountMapper.toAccountResponse(accountRepository.save(account)
        );

    }

    @Override
    public AccountResponse updateAccount(UpdateAccountRequest request) {

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND, request.getAccountId()));

        if(accountRepository.findByAccountName(request.getAccountName()).isPresent() &&
                !accountRepository.findByAccountName(request.getAccountName()).get().getAccountId().equals(request.getAccountId())) {
            throw new CustomException(ErrorCode.ACCOUNT_NAME_ALREADY_EXISTS, request.getAccountName());
        }

        if(accountRepository.findByAccountCode(request.getAccountCode()).isPresent() &&
                !accountRepository.findByAccountCode(request.getAccountCode()).get().getAccountId().equals(request.getAccountId())) {
            throw new CustomException(ErrorCode.ACCOUNT_CODE_ALREADY_EXISTS, request.getAccountCode());
        }

        accountMapper.updateAccount(account, request);
        account = accountRepository.save(account);

        return accountMapper.toAccountResponse(account);

    }

    @Override
    public Page<AccountResponse> filterAccount(String search, Pageable pageable) {
        return accountRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accountName")), like),
                        cb.like(cb.lower(root.get("accountCode")), like),
                        cb.like(cb.lower(root.get("owner").get("fullName")), like),
                        cb.like(cb.lower(root.get("owner").get("eid")), like),
                        cb.like(cb.lower(root.get("createdBy").get("fullName")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(accountMapper::toAccountResponse);
    }


}