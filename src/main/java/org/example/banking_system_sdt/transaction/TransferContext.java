package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;

public class TransferContext {

    private final TransferRequest request;
    private final AccountSnapshot source;
    private final AccountSnapshot target;
    private final BigDecimal fee;
    private final BigDecimal sentToday;

    public TransferContext(TransferRequest request, AccountSnapshot source, AccountSnapshot target,
                           BigDecimal fee, BigDecimal sentToday) {
        this.request = request;
        this.source = source;
        this.target = target;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.sentToday = sentToday != null ? sentToday : BigDecimal.ZERO;
    }

    public TransferRequest getRequest() {
        return request;
    }

    public AccountSnapshot getSource() {
        return source;
    }

    public AccountSnapshot getTarget() {
        return target;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getSentToday() {
        return sentToday;
    }
}
