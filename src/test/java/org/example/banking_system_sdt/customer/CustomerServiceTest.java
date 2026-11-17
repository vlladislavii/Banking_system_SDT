package org.example.banking_system_sdt.customer;

import org.example.banking_system_sdt.exception.BadRequestException;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.example.banking_system_sdt.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(userRepository, customerRepository);
    }

    @Test
    void register_createsAndSavesCustomer() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            return c;
        });

        Customer customer = customerService.register("John Doe", "john@example.com", "+1234567890", "123 Main St");

        assertNotNull(customer);
        assertEquals("John Doe", customer.getFullName());
        assertEquals("john@example.com", customer.getEmail());
        assertEquals("+1234567890", customer.getPhone());
        assertEquals("123 Main St", customer.getAddress());
        assertFalse(customer.isAdmin());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void register_duplicateEmailThrowsBadRequest() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(new Customer("Other", "john@example.com", "000", "addr", LocalDateTime.now())));

        assertThrows(BadRequestException.class, () ->
                customerService.register("John Doe", "john@example.com", "+1234567890", "123 Main St")
        );
        verify(customerRepository, never()).save(any());
    }

    @Test
    void getProfile_allowedForOwner() {
        Customer customer = new Customer("John Doe", "john@example.com", "123", "Main St", LocalDateTime.now());
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));

        Customer result = customerService.getProfile(10L, 10L);
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void getProfile_allowedForAdmin() {
        Administrator admin = new Administrator("Admin", "admin@bank.com", LocalDateTime.now());
        Customer customer = new Customer("John Doe", "john@example.com", "123", "Main St", LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));

        Customer result = customerService.getProfile(1L, 10L);
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void getProfile_forbiddenForOtherUser() {
        Customer otherCustomer = new Customer("Other", "other@example.com", "999", "Other St", LocalDateTime.now());
        when(userRepository.findById(20L)).thenReturn(Optional.of(otherCustomer));

        assertThrows(ForbiddenException.class, () -> customerService.getProfile(20L, 10L));
    }

    @Test
    void updateProfile_updatesCustomerFields() {
        Customer customer = new Customer("Old Name", "old@example.com", "111", "Old St", LocalDateTime.now());
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer updated = customerService.updateProfile(10L, 10L, "New Name", "new@example.com", "222", "New St");

        assertEquals("New Name", updated.getFullName());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("222", updated.getPhone());
        assertEquals("New St", updated.getAddress());
    }

    @Test
    void listCustomers_allowedForAdminOnly() {
        Administrator admin = new Administrator("Admin", "admin@bank.com", LocalDateTime.now());
        Customer customer = new Customer("User", "user@example.com", "123", "St", LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<Customer> list = customerService.listCustomers(1L);
        assertEquals(1, list.size());

        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        assertThrows(ForbiddenException.class, () -> customerService.listCustomers(2L));
    }
}
