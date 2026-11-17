package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;

public class DailyLimitHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        if (context.getRequest() == null || context.getSource() == null) {
            return ValidationResult.ok();
        }
        BigDecimal amount = context.getRequest().amount();
        BigDecimal sentToday = context.getSentToday() != null ? context.getSentToday() : BigDecimal.ZERO;
        BigDecimal totalToday = sentToday.add(amount);

        BigDecimal limit = context.getSource().dailyTransferLimit();
        if (limit != null && totalToday.compareTo(limit) > 0) {
            return ValidationResult.rejected("Daily transfer limit exceeded: limit is " + limit
                    + " EUR, already sent " + sentToday + " EUR today");
        }
        return ValidationResult.ok();
    }
}
