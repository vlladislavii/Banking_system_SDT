package org.example.banking_system_sdt.transaction;

public class AccountStatusHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        if (context.getSource() != null && !context.getSource().canSend()) {
            return ValidationResult.rejected("Source account cannot send funds (must be ACTIVE)");
        }
        if (context.getTarget() != null && !context.getTarget().canReceive()) {
            return ValidationResult.rejected("Target account cannot receive funds (CLOSED)");
        }
        return ValidationResult.ok();
    }
}
