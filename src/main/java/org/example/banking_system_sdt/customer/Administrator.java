package org.example.banking_system_sdt.customer;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("ADMINISTRATOR")
public class Administrator extends User {

    public Administrator() {
    }

    public Administrator(String fullName, String email, LocalDateTime createdAt) {
        super(fullName, email, createdAt);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
