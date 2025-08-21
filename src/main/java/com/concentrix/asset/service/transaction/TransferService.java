package com.concentrix.asset.service.transaction;

import com.concentrix.asset.dto.request.CreateTransferRequest;
import com.concentrix.asset.dto.response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TransferService {

    TransferResponse getTransferById(Integer transferId);

    TransferResponse createTransfer(CreateTransferRequest request);

    Page<TransferResponse> filterTransfers(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);

     void approveTransfer(Integer transactionId) ;

     void confirmTransfer(Integer transferId);

    Page<TransferResponse> filterTransfersSitePending(Pageable pageable) ;

    void approveTransferByToken(String token);


}

