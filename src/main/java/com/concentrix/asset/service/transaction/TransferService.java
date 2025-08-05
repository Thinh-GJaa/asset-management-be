package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface TransferService {

    TransferResponse getTransferById(Integer transferId);

    TransferResponse createTransfer(CreateTransferRequest request);

    Page<TransferResponse> filterTransfers(Integer transactionId, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    void confirmTransfer(Integer transferId);

    Page<TransferResponse> filterTransfersSitePending(Pageable pageable) ;


}

