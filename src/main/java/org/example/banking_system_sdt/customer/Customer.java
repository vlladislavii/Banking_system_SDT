package org.example.banking_system_sdt.customer;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("CUSTOMER")
public class Customer extends User {

    private String phone;
    private String address;

    public Customer() {
    }

    public Customer(String fullName, String email, String phone, String address, LocalDateTime createdAt) {
        super(fullName, email, createdAt);
        this.phone = phone;
        this.address = address;
    }

    public void updateProfile(String fullName, String email, String phone, String address) {
        setFullName(fullName);
        setEmail(email);
        this.phone = phone;
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}
