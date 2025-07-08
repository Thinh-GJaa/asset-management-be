package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnFromUserService {
    ReturnFromUserResponse getReturnFromUserById(Integer returnId);

    ReturnFromUserResponse createReturnFromUser(CreateReturnFromUserRequest request);

    Page<ReturnFromUserResponse> filterReturnFromUsers(Pageable pageable);
}