package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateDisposalRequest;
import com.concentrix.asset.dto.response.DisposalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface DisposalService {
    DisposalResponse getDisposalById(Integer disposalId);

    DisposalResponse createDisposal(CreateDisposalRequest request);

    Page<DisposalResponse> filterDisposals(Integer transactionId, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
}