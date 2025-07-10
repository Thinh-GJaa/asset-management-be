package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateDisposalRequest;
import com.concentrix.asset.dto.response.DisposalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DisposalService {
    DisposalResponse getDisposalById(Integer disposalId);

    DisposalResponse createDisposal(CreateDisposalRequest request);

    Page<DisposalResponse> filterDisposals(Pageable pageable);
}