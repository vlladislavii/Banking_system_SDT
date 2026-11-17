package org.example.banking_system_sdt.transaction;

import jakarta.validation.Valid;
import org.example.banking_system_sdt.transaction.dto.TransactionResponse;
import org.example.banking_system_sdt.transaction.dto.TransferApiRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransferService transferService;
    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransferService transferService, TransactionQueryService transactionQueryService) {
        this.transferService = transferService;
        this.transactionQueryService = transactionQueryService;
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @Valid @RequestBody TransferApiRequest request) {
        TransferRequest transferRequest = new TransferRequest(
                callerId,
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount()
        );
        Transaction tx = transferService.transfer(transferRequest);
        return TransactionResponse.from(tx);
    }

    @GetMapping("/history/{accountId}")
    public List<TransactionResponse> getAccountHistory(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long accountId) {
        return transactionQueryService.getAccountHistory(callerId, accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping("/search")
    public List<TransactionResponse> search(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestBody(required = false) TransactionFilter filter) {
        return transactionQueryService.search(callerId, filter).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
