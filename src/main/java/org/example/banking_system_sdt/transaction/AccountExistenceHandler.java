package org.example.banking_system_sdt.transaction;

public class AccountExistenceHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        if (context.getSource() == null) {
            return ValidationResult.rejected("Source account does not exist");
        }
        if (context.getTarget() == null) {
            return ValidationResult.rejected("Target account does not exist");
        }
        return ValidationResult.ok();
    }
}
