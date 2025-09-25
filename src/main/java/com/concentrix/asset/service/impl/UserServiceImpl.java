package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.request.UserImportRequest;
import com.concentrix.asset.dto.response.TransactionItemsResponse;
import com.concentrix.asset.dto.response.TransactionResponse;
import com.concentrix.asset.dto.response.UserResponse;
import com.concentrix.asset.entity.AssetTransaction;
import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.Role;
import com.concentrix.asset.exception.CustomException;
import com.concentrix.asset.exception.ErrorCode;
import com.concentrix.asset.mapper.TransactionMapper;
import com.concentrix.asset.mapper.UserMapper;
import com.concentrix.asset.repository.TransactionRepository;
import com.concentrix.asset.repository.UserRepository;
import com.concentrix.asset.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    TransactionRepository transactionRepository;
    TransactionMapper transactionMapper;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${app.password.default}")
    String defaultPassword;

    @Override
    public UserResponse getUserById(String eid) {
        User user = userRepository.findById(eid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, eid));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        userRepository.findById(request.getEid()).ifPresent(
                user -> {
                    throw new CustomException(ErrorCode.USER_ALREADY_EXISTS, request.getEid());
                });

        if (userRepository.findBySso(request.getSso()).isPresent()) {
            throw new CustomException(ErrorCode.SSO_ALREADY_EXISTS, request.getSso());
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, request.getEmail());
        }

        User user = userMapper.toUser(request);

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    private void setRole(User user, Role role, boolean createPassword) {
        user.setRole(role);
        if (createPassword) {
            user.setPassword(passwordEncoder.encode(defaultPassword));
        }
    }


    @Override
    public UserResponse updateUser(UpdateUserRequest request) {

        User user = userRepository.findById(request.getEid())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, request.getEid()));

        Optional<User> existingEmail = userRepository.findByEmail(request.getEmail());
        if (existingEmail.isPresent() && !existingEmail.get().getEid().equals(request.getEid())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, request.getEmail());
        }

        Optional<User> existingSSO = userRepository.findBySso(request.getSso());
        if (existingSSO.isPresent() && !existingSSO.get().getEid().equals(request.getEid())) {
            throw new CustomException(ErrorCode.SSO_ALREADY_EXISTS, request.getSso());
        }

        user = userMapper.updateUser(user, request);

        // Keep existing password for admin users
        setRole(user, request.getRole(), request.getRole() != Role.OTHER && user.getPassword() == null);

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public Page<UserResponse> filterUser(String search, Role role, Integer accountId, Pageable pageable) {
        return userRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("eid")), like),
                        cb.like(cb.lower(root.get("sso")), like),
                        cb.like(cb.lower(root.get("msa")), like)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("accountId"), accountId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(userMapper::toUserResponse);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, email));
        return userMapper.toUserResponse(user);
    }

    @Override
    public Map<String, Object> importUsers(List<UserImportRequest> importRequests) {
        int created = 0;
        int updated = 0;
        List<User> users = new ArrayList<>();

        for (UserImportRequest req : importRequests) {
            if (req.getEid() == null || req.getEmail() == null)
                continue;

            // Nếu eid đã tồn tại: cập nhật các trường ánh xạ
            var userOpt = userRepository.findById(req.getEid());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user = userMapper.updateUser(user, req);
                users.add(user);
                updated++;
            } else {
                User user = userMapper.ToUser(req);
                users.add(user);
                created++;
            }
        }

        userRepository.saveAll(users);

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        return result;
    }

    @Override
    public List<TransactionResponse> getUserTransactions(String eid) {

        userRepository.findById(eid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, eid));

        List<AssetTransaction> transactions = transactionRepository.findAllByUserUse_Eid(eid);

        transactions.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));

        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    public List<TransactionItemsResponse> getUserTransactionItems(Integer transactionId) {
        AssetTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transactionId));
        return transaction.getDetails().stream()
                .map(transactionMapper::toTransactionItemsResponse)
                .toList();
    }

    @Override
    public User getCurrentUser() {
        String EID = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
    }

}
