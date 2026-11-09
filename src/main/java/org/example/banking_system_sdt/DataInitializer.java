package org.example.banking_system_sdt;

import org.example.banking_system_sdt.customer.Administrator;
import org.example.banking_system_sdt.customer.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@bank.com").isEmpty()) {
            Administrator admin = new Administrator("System Admin", "admin@bank.com", LocalDateTime.now());
            userRepository.save(admin);
        }
    }
}
