package org.example.banking_system_sdt.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransferValidationHandlerTest {

    private TransferValidationHandler chain;

    @BeforeEach
    void setUp() {
        TransferValidationHandler h1 = new PositiveAmountHandler();
        TransferValidationHandler h2 = new DifferentAccountsHandler();
        TransferValidationHandler h3 = new AccountExistenceHandler();
        TransferValidationHandler h4 = new AuthorizationHandler();
        TransferValidationHandler h5 = new AccountStatusHandler();
        TransferValidationHandler h6 = new SufficientFundsHandler();
        TransferValidationHandler h7 = new DailyLimitHandler();

        h1.setNext(h2);
        h2.setNext(h3);
        h3.setNext(h4);
        h4.setNext(h5);
        h5.setNext(h6);
        h6.setNext(h7);

        chain = h1;
    }

    private AccountSnapshot createSource(BigDecimal balance, BigDecimal dailyLimit, boolean canSend) {
        return new AccountSnapshot(1L, 10L, Set.of(15L), canSend, true, balance, dailyLimit);
    }

    private AccountSnapshot createTarget(boolean canReceive) {
        return new AccountSnapshot(2L, 20L, Set.of(), true, canReceive, new BigDecimal("100.00"), new BigDecimal("1000.00"));
    }

    @Test
    void chain_allValid_returnsOk() {
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("100.00"));
        TransferContext context = new TransferContext(
                request,
                createSource(new BigDecimal("500.00"), new BigDecimal("1000.00"), true),
                createTarget(true),
                new BigDecimal("1.00"),
                BigDecimal.ZERO
        );

        ValidationResult result = chain.validate(context);
        assertTrue(result.valid());
        assertNull(result.reason());
    }

    @Test
    void positiveAmountHandler_rejectsZeroOrNegative() {
        TransferRequest zeroRequest = new TransferRequest(10L, 1L, 2L, BigDecimal.ZERO);
        TransferContext context = new TransferContext(zeroRequest, null, null, BigDecimal.ZERO, BigDecimal.ZERO);

        ValidationResult result = chain.validate(context);
        assertFalse(result.valid());
        assertTrue(result.reason().contains("positive"));
    }

    @Test
    void differentAccountsHandler_rejectsSameAccount() {
        TransferRequest request = new TransferRequest(10L, 1L, 1L, new BigDecimal("50.00"));
        TransferContext context = new TransferContext(request, null, null, BigDecimal.ZERO, BigDecimal.ZERO);

        ValidationResult result = chain.validate(context);
        assertFalse(result.valid());
        assertTrue(result.reason().contains("different"));
    }

    @Test
    void accountExistenceHandler_rejectsMissingAccounts() {
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("50.00"));

        TransferContext missingSource = new TransferContext(request, null, createTarget(true), BigDecimal.ZERO, BigDecimal.ZERO);
        ValidationResult res1 = chain.validate(missingSource);
        assertFalse(res1.valid());
        assertTrue(res1.reason().contains("Source account does not exist"));

        TransferContext missingTarget = new TransferContext(request, createSource(new BigDecimal("100.00"), new BigDecimal("1000.00"), true), null, BigDecimal.ZERO, BigDecimal.ZERO);
        ValidationResult res2 = chain.validate(missingTarget);
        assertFalse(res2.valid());
        assertTrue(res2.reason().contains("Target account does not exist"));
    }

    @Test
    void authorizationHandler_rejectsUnauthorizedCaller() {
        // Caller 99L is neither owner (10L) nor authorized (15L)
        TransferRequest request = new TransferRequest(99L, 1L, 2L, new BigDecimal("50.00"));
        TransferContext context = new TransferContext(
                request,
                createSource(new BigDecimal("500.00"), new BigDecimal("1000.00"), true),
                createTarget(true),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        ValidationResult result = chain.validate(context);
        assertFalse(result.valid());
        assertTrue(result.reason().contains("not authorized"));

        // Authorized customer (15L) is allowed
        TransferRequest authorizedRequest = new TransferRequest(15L, 1L, 2L, new BigDecimal("50.00"));
        TransferContext authContext = new TransferContext(
                authorizedRequest,
                createSource(new BigDecimal("500.00"), new BigDecimal("1000.00"), true),
                createTarget(true),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        ValidationResult authResult = chain.validate(authContext);
        assertTrue(authResult.valid());
    }

    @Test
    void accountStatusHandler_rejectsInactiveAccounts() {
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("50.00"));

        // Source frozen (canSend = false)
        TransferContext frozenSource = new TransferContext(
                request,
                createSource(new BigDecimal("500.00"), new BigDecimal("1000.00"), false),
                createTarget(true),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        ValidationResult res1 = chain.validate(frozenSource);
        assertFalse(res1.valid());
        assertTrue(res1.reason().contains("cannot send funds"));

        // Target closed (canReceive = false)
        TransferContext closedTarget = new TransferContext(
                request,
                createSource(new BigDecimal("500.00"), new BigDecimal("1000.00"), true),
                createTarget(false),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        ValidationResult res2 = chain.validate(closedTarget);
        assertFalse(res2.valid());
        assertTrue(res2.reason().contains("cannot receive funds"));
    }

    @Test
    void sufficientFundsHandler_rejectsWhenFundsInsufficient() {
        // Available: 100.00, Transfer: 100.00, Fee: 1.00 -> Required: 101.00
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("100.00"));
        TransferContext context = new TransferContext(
                request,
                createSource(new BigDecimal("100.00"), new BigDecimal("1000.00"), true),
                createTarget(true),
                new BigDecimal("1.00"),
                BigDecimal.ZERO
        );

        ValidationResult result = chain.validate(context);
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Insufficient funds"));
    }

    @Test
    void dailyLimitHandler_rejectsWhenLimitExceeded() {
        // Daily limit: 500.00, Already sent today: 450.00, Current transfer: 100.00 -> 550.00
        TransferRequest request = new TransferRequest(10L, 1L, 2L, new BigDecimal("100.00"));
        TransferContext context = new TransferContext(
                request,
                createSource(new BigDecimal("1000.00"), new BigDecimal("500.00"), true),
                createTarget(true),
                BigDecimal.ZERO,
                new BigDecimal("450.00")
        );

        ValidationResult result = chain.validate(context);
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Daily transfer limit exceeded"));
    }
}
