package com.concentrix.asset.service.impl;

import com.concentrix.asset.dto.request.CreateUserRequest;
import com.concentrix.asset.dto.request.UpdateUserRequest;
import com.concentrix.asset.dto.request.UserImportRequest;
import com.concentrix.asset.dto.response.DeviceBorrowingInfoResponse;
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
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public UserResponse getUserById(String eid) {
        User user = userRepository.findById(eid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, eid));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        userRepository.findById(request.getEid())
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.USER_ALREADY_EXISTS, request.getEid());
                });

        if (userRepository.findBySso(request.getSSO()).isPresent()) {
            throw new CustomException(ErrorCode.SSO_ALREADY_EXISTS, request.getSSO());
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, request.getEmail());
        }

        User user = userMapper.toUser(request);
        if (request.getRole() == Role.ADMIN) {
            user = setRoleOther(user);
        } else {
            user = setRole(user, request.getRole(), true); // Create password for non-admin users
        }
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    private User setRole(User user, Role role, boolean createPassword) {
        user.setRole(role);
        if (createPassword) {
            user.setPassword(passwordEncoder.encode(user.getSso())); // Clear password for non-admin users
        }
        return user;
    }

    private User setRoleOther(User user) {
        user.setRole(Role.OTHER);
        return user;
    }

    @Override
    public UserResponse updateUser(UpdateUserRequest request) {

        var context = SecurityContextHolder.getContext();
        String EID = context.getAuthentication().getName();

        userRepository.findById(EID)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, EID));
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

        if (request.getRole() != Role.OTHER)
            user = setRole(user, request.getRole(), true); // Update password only for non-admin users

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public Page<UserResponse> filterUser(String search, Role role, Pageable pageable) {
        return userRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("sso")), like),
                        cb.like(cb.lower(root.get("msa")), like)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
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
        List<String> emailErrors = new java.util.ArrayList<>();
        for (UserImportRequest req : importRequests) {
            if (req.getEmail() == null || req.getEid() == null)
                continue;
            // Check email trùng với user khác eid
            var emailUserOpt = userRepository.findByEmail(req.getEmail());
            if (emailUserOpt.isPresent() && !emailUserOpt.get().getEid().equals(req.getEid())) {
                emailErrors.add(req.getEmail());
                continue;
            }
            // Nếu eid đã tồn tại: cập nhật các trường ánh xạ
            var userOpt = userRepository.findById(req.getEid());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setFullName(req.getFullName());
                user.setJobTitle(req.getJobTitle());
                user.setEmail(req.getEmail());
                user.setSso(req.getSso());
                user.setMsa(req.getMsa());
                user.setLocation(req.getLocation());
                user.setCompany(req.getCompany());
                user.setCostCenter(req.getCostCenter());
                user.setMsaClient(req.getMsaClient());
                user.setManagerEmail(req.getManagerEmail());
                if (req.getIsActive() != null)
                    user.setActive(req.getIsActive());
                userRepository.save(user);
                updated++;
            } else {
                // Tạo mới user
                User user = User.builder()
                        .eid(req.getEid())
                        .fullName(req.getFullName())
                        .jobTitle(req.getJobTitle())
                        .email(req.getEmail())
                        .sso(req.getSso())
                        .msa(req.getMsa())
                        .location(req.getLocation())
                        .company(req.getCompany())
                        .costCenter(req.getCostCenter())
                        .msaClient(req.getMsaClient())
                        .managerEmail(req.getManagerEmail())
                        .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                        .build();
                userRepository.save(user);
                created++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("emailErrors", emailErrors);
        return result;
    }

    @Override
    public List<TransactionResponse> getUserTransactions(String eid) {

        User user = userRepository.findById(eid)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, eid));

        List<AssetTransaction> transactions = transactionRepository.findAllByUserUse_Eid(eid);

        transactions.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));

        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    public List<TransactionItemsResponse> getUserTransactionItems(Integer transactionId) {
        AssetTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRANSACTION_NOT_FOUND, transactionId.toString()));
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
