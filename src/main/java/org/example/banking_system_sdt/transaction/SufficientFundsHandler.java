package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;

public class SufficientFundsHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        if (context.getRequest() == null || context.getSource() == null) {
            return ValidationResult.ok();
        }
        BigDecimal amount = context.getRequest().amount();
        BigDecimal fee = context.getFee() != null ? context.getFee() : BigDecimal.ZERO;
        BigDecimal totalRequired = amount.add(fee);

        if (context.getSource().availableToSpend().compareTo(totalRequired) < 0) {
            return ValidationResult.rejected("Insufficient funds: required " + totalRequired + " EUR but available is "
                    + context.getSource().availableToSpend() + " EUR");
        }
        return ValidationResult.ok();
    }
}
