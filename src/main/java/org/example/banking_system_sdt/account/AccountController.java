package org.example.banking_system_sdt.account;

import jakarta.validation.Valid;
import org.example.banking_system_sdt.account.dto.AccountResponse;
import org.example.banking_system_sdt.account.dto.OpenAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse openAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @Valid @RequestBody OpenAccountRequest request) {
        Account account = accountService.openAccount(
                callerId,
                request.ownerId(),
                request.type(),
                request.initialDeposit()
        );
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id) {
        Account account = accountService.getAccount(callerId, id);
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}/balance")
    public Map<String, Object> getBalance(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id) {
        BigDecimal balance = accountService.getBalance(callerId, id);
        return Map.of("accountId", id, "balance", balance, "currency", "EUR");
    }

    @GetMapping
    public List<AccountResponse> listCustomerAccounts(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestParam Long customerId) {
        return accountService.listCustomerAccounts(callerId, customerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PostMapping("/{id}/waive-fees")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void waiveFees(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id) {
        accountService.waiveFees(callerId, id);
    }
}
