package org.example.banking_system_sdt.transaction;

import java.math.BigDecimal;

public class PositiveAmountHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        if (context.getRequest() == null || context.getRequest().amount() == null
                || context.getRequest().amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.rejected("Transfer amount must be positive");
        }
        return ValidationResult.ok();
    }
}
