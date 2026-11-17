package org.example.banking_system_sdt.transaction;

public class DifferentAccountsHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        TransferRequest request = context.getRequest();
        if (request != null && request.sourceAccountId() != null
                && request.sourceAccountId().equals(request.targetAccountId())) {
            return ValidationResult.rejected("Source and target accounts must be different");
        }
        return ValidationResult.ok();
    }
}
