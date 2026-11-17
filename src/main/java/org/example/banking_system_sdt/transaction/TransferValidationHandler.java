package org.example.banking_system_sdt.transaction;

public abstract class TransferValidationHandler {

    private TransferValidationHandler next;

    public TransferValidationHandler setNext(TransferValidationHandler next) {
        this.next = next;
        return next;
    }

    public ValidationResult validate(TransferContext context) {
        ValidationResult result = check(context);
        if (result != null && !result.valid()) {
            return result;
        }
        if (next != null) {
            return next.validate(context);
        }
        return ValidationResult.ok();
    }

    protected abstract ValidationResult check(TransferContext context);

    public TransferValidationHandler getNext() {
        return next;
    }
}
