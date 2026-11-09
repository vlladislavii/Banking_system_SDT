package org.example.banking_system_sdt.customer;

import org.example.banking_system_sdt.exception.BadRequestException;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.example.banking_system_sdt.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public CustomerService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public Customer register(String fullName, String email, String phone, String address) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already registered: " + email);
        }
        Customer customer = new Customer(fullName, email, phone, address, LocalDateTime.now());
        return customerRepository.save(customer);
    }

    public Customer updateProfile(Long callerId, Long customerId, String fullName, String email, String phone, String address) {
        if (!isAdmin(callerId) && (callerId == null || !callerId.equals(customerId))) {
            throw new ForbiddenException("Caller is not authorized to update this profile");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));
        customer.updateProfile(fullName, email, phone, address);
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer getProfile(Long callerId, Long customerId) {
        if (!isAdmin(callerId) && (callerId == null || !callerId.equals(customerId))) {
            throw new ForbiddenException("Caller is not authorized to view this profile");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers(Long callerId) {
        if (!isAdmin(callerId)) {
            throw new ForbiddenException("Only administrators can list customers");
        }
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(User::isAdmin)
                .orElse(false);
    }
}
