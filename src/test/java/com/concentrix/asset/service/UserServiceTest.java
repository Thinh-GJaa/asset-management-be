package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.request.UserImportRequest;
import com.concentrix.asset.dto.response.TransactionItemsResponse;
import com.concentrix.asset.dto.response.TransactionResponse;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.TransactionDetail;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransactionMapper;
import com.concentrix.asset.mapper.UserMapper;
import com.concentrix.asset.repository.AccountRepository;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.impl.UserServiceImpl;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountRepository accountRepository;

    @InjectMocks private UserServiceImpl service;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void init() {
        user = new User();
        user.setEid("E1");
        user.setEmail("e1@example.com");
        user.setRole(Role.OTHER);

        userResponse = new UserResponse();
        userResponse.setEid("E1");
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse res = service.getUserById("E1");
        assertEquals("E1", res.getEid());
        verify(userRepository).findById("E1");
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById("X")).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getUserById("X"));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createUser_duplicateEid() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEid("E1");
        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        CustomException ex = assertThrows(CustomException.class, () -> service.createUser(req));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void createUser_duplicateSso() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEid("E2");
        req.setSso("sso");
        req.setEmail("e2@example.com");
        when(userRepository.findById("E2")).thenReturn(Optional.empty());
        when(userRepository.findBySso("sso")).thenReturn(Optional.of(new User()));
        CustomException ex = assertThrows(CustomException.class, () -> service.createUser(req));
        assertEquals(ErrorCode.SSO_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void createUser_duplicateEmail() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEid("E2");
        req.setSso("sso");
        req.setEmail("e2@example.com");
        when(userRepository.findById("E2")).thenReturn(Optional.empty());
        when(userRepository.findBySso("sso")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("e2@example.com")).thenReturn(Optional.of(new User()));
        CustomException ex = assertThrows(CustomException.class, () -> service.createUser(req));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void createUser_success() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEid("E2");
        req.setSso("sso2");
        req.setEmail("e2@example.com");
        when(userRepository.findById("E2")).thenReturn(Optional.empty());
        when(userRepository.findBySso("sso2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("e2@example.com")).thenReturn(Optional.empty());
        when(userMapper.toUser(req)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse res = service.createUser(req);
        assertEquals("E1", res.getEid());
        verify(userRepository).save(user);
    }

    private void setSecurityContextEid(String eid) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(eid);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void updateUser_currentUserMissing_throws() {
        setSecurityContextEid("CURR");
        when(userRepository.findById("CURR")).thenReturn(Optional.empty());
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEid("E2");
        CustomException ex = assertThrows(CustomException.class, () -> service.updateUser(req));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateUser_targetUserMissing_throws() {
        setSecurityContextEid("CURR");
        when(userRepository.findById("CURR")).thenReturn(Optional.of(new User()));
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEid("E2");
        when(userRepository.findById("E2")).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.updateUser(req));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateUser_duplicateEmail_throws() {
        setSecurityContextEid("CURR");
        when(userRepository.findById("CURR")).thenReturn(Optional.of(new User()));
        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEid("E1");
        req.setEmail("dup@example.com");
        User other = new User(); other.setEid("OTHER");
        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(other));
        CustomException ex = assertThrows(CustomException.class, () -> service.updateUser(req));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void updateUser_duplicateSso_throws() {
        setSecurityContextEid("CURR");
        when(userRepository.findById("CURR")).thenReturn(Optional.of(new User()));
        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEid("E1");
        req.setSso("dup");
        User other = new User(); other.setEid("OTHER");
        when(userRepository.findBySso("dup")).thenReturn(Optional.of(other));
        CustomException ex = assertThrows(CustomException.class, () -> service.updateUser(req));
        assertEquals(ErrorCode.SSO_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void updateUser_success_setsRoleAndPasswordWhenNeeded() {
        setSecurityContextEid("CURR");
        when(userRepository.findById("CURR")).thenReturn(Optional.of(new User()));
        // target user with null password triggers password creation when role != OTHER
        User target = new User(); target.setEid("E1"); target.setPassword(null);
        when(userRepository.findById("E1")).thenReturn(Optional.of(target));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setEid("E1");
        req.setRole(Role.ADMIN);

        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());
        when(userRepository.findBySso(null)).thenReturn(Optional.empty());
        when(userMapper.updateUser(target, req)).thenReturn(target);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        when(userRepository.save(target)).thenReturn(target);
        when(userMapper.toUserResponse(target)).thenReturn(userResponse);

        UserResponse res = service.updateUser(req);
        assertEquals("E1", res.getEid());
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(target);
    }

    @Test
    void filterUser_buildsPredicates_andMaps() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable))).thenReturn(page);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        Page<UserResponse> res = service.filterUser("e1", Role.OTHER, 1, pageable);
        assertEquals(1, res.getTotalElements());
        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable));
    }

    @Test
    void getUserByEmail_success_and_notFound() {
        when(userRepository.findByEmail("e1@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);
        UserResponse res = service.getUserByEmail("e1@example.com");
        assertEquals("E1", res.getEid());

        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getUserByEmail("x@example.com"));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void importUsers_countsCreatedAndUpdated_andSavesAll() {
        UserImportRequest skip1 = new UserImportRequest(); // missing eid/email -> skipped
        UserImportRequest updReq = new UserImportRequest(); updReq.setEid("E1"); updReq.setEmail("e1@example.com");
        UserImportRequest newReq = new UserImportRequest(); newReq.setEid("E2"); newReq.setEmail("e2@example.com");

        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        when(userRepository.findById("E2")).thenReturn(Optional.empty());

        User updated = new User(); updated.setEid("E1");
        when(userMapper.updateUser(eq(user), eq(updReq))).thenReturn(updated);
        User created = new User(); created.setEid("E2");
        when(userMapper.ToUser(eq(newReq))).thenReturn(created);

        Map<String, Object> result = service.importUsers(List.of(skip1, updReq, newReq));

        verify(userRepository).saveAll(argThat(iterable -> {
            if (iterable == null) return false;
            int count = 0;
            Iterator<?> it = iterable.iterator();
            while (it.hasNext()) { it.next(); count++; }
            return count == 2;
        }));
        assertEquals(1, result.get("created"));
        assertEquals(1, result.get("updated"));
    }

    @Test
    void getUserTransactions_sortsAndMaps() {
        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        AssetTransaction t1 = new AssetTransaction(); t1.setCreatedAt(LocalDateTime.now().minusDays(1));
        AssetTransaction t2 = new AssetTransaction(); t2.setCreatedAt(LocalDateTime.now());
        when(transactionRepository.findAllByUserUse_Eid("E1")).thenReturn(List.of(t1, t2));
        TransactionResponse tr1 = new TransactionResponse();
        TransactionResponse tr2 = new TransactionResponse();
        when(transactionMapper.toTransactionResponse(t1)).thenReturn(tr1);
        when(transactionMapper.toTransactionResponse(t2)).thenReturn(tr2);

        List<TransactionResponse> res = service.getUserTransactions("E1");
        assertEquals(2, res.size());
    }

    @Test
    void getUserTransactions_userNotFound() {
        when(userRepository.findById("X")).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getUserTransactions("X"));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getUserTransactionItems_mapsDetails_orThrows() {
        AssetTransaction tx = new AssetTransaction();
        TransactionDetail d = new TransactionDetail();
        tx.setDetails(new ArrayList<>(List.of(d)));
        when(transactionRepository.findById(10)).thenReturn(Optional.of(tx));
        when(transactionMapper.toTransactionItemsResponse(d)).thenReturn(new TransactionItemsResponse());
        List<TransactionItemsResponse> res = service.getUserTransactionItems(10);
        assertEquals(1, res.size());

        when(transactionRepository.findById(11)).thenReturn(Optional.empty());
        CustomException ex = assertThrows(CustomException.class, () -> service.getUserTransactionItems(11));
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getCurrentUser_readsFromSecurityContext() {
        setSecurityContextEid("E1");
        when(userRepository.findById("E1")).thenReturn(Optional.of(user));
        User res = service.getCurrentUser();
        assertEquals("E1", res.getEid());
    }
}


