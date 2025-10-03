package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateReturnFromUserRequest;
import com.concentrix.asset.dto.response.AssetHandoverResponse;
import com.concentrix.asset.dto.response.ReturnFromUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface ReturnFromUserService {
    ReturnFromUserResponse getReturnFromUserById(Integer returnId);

    ReturnFromUserResponse createReturnFromUser(CreateReturnFromUserRequest request);

    Page<ReturnFromUserResponse> filterReturnFromUsers(String search, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);

    AssetHandoverResponse getAssetHandoverForm(Integer id);

    void uploadImage(Integer returnId, List<MultipartFile> images);

}