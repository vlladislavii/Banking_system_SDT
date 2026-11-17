package org.example.banking_system_sdt.transaction;

public class AuthorizationHandler extends TransferValidationHandler {

    @Override
    protected ValidationResult check(TransferContext context) {
        Long callerId = context.getRequest() != null ? context.getRequest().callerId() : null;
        if (callerId == null || context.getSource() == null || !context.getSource().hasAccess(callerId)) {
            return ValidationResult.rejected("Caller is not authorized to transfer funds from source account");
        }
        return ValidationResult.ok();
    }
}
