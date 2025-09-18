package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateAccountRequest;
import com.concentrix.asset.dto.request.UpdateAccountRequest;
import com.concentrix.asset.dto.response.AccountResponse;
import com.concentrix.asset.entity.Account;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.AccountMapper;
import com.concentrix.asset.repository.AccountRepository;
import com.concentrix.asset.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account account;
    private User user;
    private User currentUser;
    private AccountResponse accountResponse;
    private CreateAccountRequest createRequest;
    private UpdateAccountRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Setup User
        user = new User();
        user.setEid("E123");
        user.setFullName("Test User");

        currentUser = new User();
        currentUser.setEid("E456");
        currentUser.setFullName("Current User");

        // Setup Account
        account = new Account();
        account.setAccountId(1);
        account.setAccountName("TestAccount");
        account.setAccountCode("TEST123");
        account.setOwner(user);
        account.setDescription("Test Description");
        account.setCreatedBy(currentUser);

        // Setup AccountResponse
        accountResponse = new AccountResponse();
        accountResponse.setAccountId(1);
        accountResponse.setAccountName("TestAccount");
        accountResponse.setAccountCode("TEST123");

        // Setup CreateAccountRequest
        createRequest = new CreateAccountRequest();
        createRequest.setAccountName("TestAccount");
        createRequest.setAccountCode("TEST123");
        createRequest.setOwnerEid("E123");
        createRequest.setDescription("Test Description");

        // Setup UpdateAccountRequest
        updateRequest = new UpdateAccountRequest();
        updateRequest.setAccountId(1);
        updateRequest.setAccountName("UpdatedAccount");
        updateRequest.setAccountCode("UPDATE123");
        updateRequest.setOwnerEid("E123");
        updateRequest.setDescription("Updated Description");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getAccountById_validId_returnAccountResponse() {
        // Given
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        AccountResponse result = accountService.getAccountById(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        assertEquals("TestAccount", result.getAccountName());
        assertEquals("TEST123", result.getAccountCode());
        verify(accountRepository).findById(1);
        verify(accountMapper).toAccountResponse(account);
    }

    @Test
    void getAccountById_invalidId_throwCustomException() {
        // Given
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class, 
            () -> accountService.getAccountById(999));

        //Then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("999"));
        verify(accountRepository).findById(999);
        verify(accountMapper, never()).toAccountResponse(any());
    }

    @Test
    void createAccount_validRequest_returnAccountResponse() {
        // Given
        when(accountRepository.findByAccountName("TestAccount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("TEST123")).thenReturn(Optional.empty());
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(accountMapper.toAccount(createRequest)).thenReturn(account);
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        AccountResponse result = accountService.createAccount(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        assertEquals("TestAccount", result.getAccountName());
        verify(accountRepository).findByAccountName("TestAccount");
        verify(accountRepository).findByAccountCode("TEST123");
        verify(userService).getCurrentUser();
        verify(accountMapper).toAccount(createRequest);
        verify(accountRepository).save(account);
        verify(accountMapper).toAccountResponse(account);
    }

    @Test
    void createAccount_accountNameExists_throwCustomException() {
        // Given
        when(accountRepository.findByAccountName("TestAccount")).thenReturn(Optional.of(account));

        // When
        CustomException exception = assertThrows(CustomException.class, 
            () -> accountService.createAccount(createRequest));

        //Then
        assertEquals(ErrorCode.ACCOUNT_NAME_ALREADY_EXISTS, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("TestAccount"));
        verify(accountRepository).findByAccountName("TestAccount");
        verify(accountRepository, never()).findByAccountCode(any());
        verify(accountMapper, never()).toAccount(any());
    }

    @Test
    void createAccount_accountCodeExists_throwCustomException() {
        // Given
        when(accountRepository.findByAccountName("TestAccount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("TEST123")).thenReturn(Optional.of(account));

        // When
        CustomException exception = assertThrows(CustomException.class, 
            () -> accountService.createAccount(createRequest));

        //Then
        assertEquals(ErrorCode.ACCOUNT_CODE_ALREADY_EXISTS, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("TEST123"));
        verify(accountRepository).findByAccountName("TestAccount");
        verify(accountRepository).findByAccountCode("TEST123");
        verify(accountMapper, never()).toAccount(any());
    }

    @Test
    void updateAccount_validRequest_returnAccountResponse() {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setAccountId(1);
        updatedAccount.setAccountName("UpdatedAccount");
        updatedAccount.setAccountCode("UPDATE123");

        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountName("UpdatedAccount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("UPDATE123")).thenReturn(Optional.empty());
        when(accountRepository.save(account)).thenReturn(updatedAccount);
        when(accountMapper.toAccountResponse(updatedAccount)).thenReturn(accountResponse);

        // When
        AccountResponse result = accountService.updateAccount(updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        verify(accountRepository).findById(1);
        verify(accountRepository).findByAccountName("UpdatedAccount");
        verify(accountRepository).findByAccountCode("UPDATE123");
        verify(accountMapper).updateAccount(account, updateRequest);
        verify(accountRepository).save(account);
        verify(accountMapper).toAccountResponse(updatedAccount);
    }

    @Test
    void updateAccount_accountNotFound_throwCustomException() {
        // Given
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class, 
            () -> accountService.updateAccount(updateRequest));

        // Then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("999"));
        verify(accountRepository).findById(999);
        verify(accountMapper, never()).updateAccount(any(), any());
    }

    @Test
    void updateAccount_accountNameExistsForDifferentAccount_throwCustomException() {
        // Given
        Account existingAccount = new Account();
        existingAccount.setAccountId(2); // Different account ID
        existingAccount.setAccountName("UpdatedAccount");

        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountName("UpdatedAccount")).thenReturn(Optional.of(existingAccount));

        // When
        CustomException exception = assertThrows(CustomException.class, 
            () -> accountService.updateAccount(updateRequest));

        // Then
        assertEquals(ErrorCode.ACCOUNT_NAME_ALREADY_EXISTS, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("UpdatedAccount"));
        verify(accountRepository).findById(1);
        verify(accountRepository).findByAccountName("UpdatedAccount");
        verify(accountMapper, never()).updateAccount(any(), any());
    }

    @Test
    void updateAccount_accountCodeExistsForDifferentAccount_throwCustomException() {
        // Given
        Account existingAccount = new Account();
        existingAccount.setAccountId(2); // Different account ID
        existingAccount.setAccountCode("UPDATE123");

        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountName("UpdatedAccount")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("UPDATE123")).thenReturn(Optional.of(existingAccount));

        // When
        CustomException exception = assertThrows(CustomException.class, 
            () -> accountService.updateAccount(updateRequest));

        // Then
        assertEquals(ErrorCode.ACCOUNT_CODE_ALREADY_EXISTS, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("UPDATE123"));
        verify(accountRepository).findById(1);
        verify(accountRepository).findByAccountName("UpdatedAccount");
        verify(accountRepository).findByAccountCode("UPDATE123");
        verify(accountMapper, never()).updateAccount(any(), any());
    }

    @Test
    void updateAccount_sameAccountNameAndCode_success() {
        // Given
        updateRequest.setAccountName("TestAccount"); // Same as existing
        updateRequest.setAccountCode("TEST123"); // Same as existing

        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountName("TestAccount")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountCode("TEST123")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        AccountResponse result = accountService.updateAccount(updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        verify(accountRepository).findById(1);
//        verify(accountRepository).findByAccountName("TestAccount");
//        verify(accountRepository).findByAccountCode("TEST123");
        verify(accountMapper).updateAccount(account, updateRequest);
        verify(accountRepository).save(account);
        verify(accountMapper).toAccountResponse(account);
    }

    @Test
    void filterAccount_withSearchTerm_returnFilteredResults() {
        // Given
        List<Account> accounts = Arrays.asList(account);
        Page<Account> accountPage = new PageImpl<>(accounts, pageable, 1);
        Page<AccountResponse> expectedResponse = new PageImpl<>(Arrays.asList(accountResponse), pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(accountPage);
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        Page<AccountResponse> result = accountService.filterAccount("test", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(accountResponse, result.getContent().get(0));
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
        verify(accountMapper).toAccountResponse(account);
    }

    @Test
    void filterAccount_withoutSearchTerm_returnAllResults() {
        // Given
        List<Account> accounts = Arrays.asList(account);
        Page<Account> accountPage = new PageImpl<>(accounts, pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(accountPage);
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        Page<AccountResponse> result = accountService.filterAccount(null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(accountResponse, result.getContent().get(0));
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
        verify(accountMapper).toAccountResponse(account);
    }

    @Test
    void filterAccount_emptySearchTerm_returnAllResults() {
        // Given
        List<Account> accounts = Collections.singletonList(account);
        Page<Account> accountPage = new PageImpl<>(accounts, pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(accountPage);
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        Page<AccountResponse> result = accountService.filterAccount("", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(accountResponse, result.getContent().get(0));
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
        verify(accountMapper).toAccountResponse(account);
    }

    @Test
    void filterAccount_whitespaceSearchTerm_returnAllResults() {
        // Given
        List<Account> accounts = Arrays.asList(account);
        Page<Account> accountPage = new PageImpl<>(accounts, pageable, 1);

        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(accountPage);
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);

        // When
        Page<AccountResponse> result = accountService.filterAccount("   ", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(accountResponse, result.getContent().get(0));
        verify(accountRepository).findAll(any(Specification.class), eq(pageable));
        verify(accountMapper).toAccountResponse(account);
    }
}
