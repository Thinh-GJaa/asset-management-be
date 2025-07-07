package com.concentrix.asset.service;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransferService {

    TransferResponse getTransferById(Integer transferId);

    TransferResponse createTransfer(CreateTransferRequest request);

    Page<TransferResponse> filterTransfers(Pageable pageable);

}

