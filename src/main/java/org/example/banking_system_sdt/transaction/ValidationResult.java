package org.example.banking_system_sdt.transaction;

public record ValidationResult(boolean valid, String reason) {

    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult rejected(String reason) {
        return new ValidationResult(false, reason);
    }
}
