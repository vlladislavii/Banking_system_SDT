package org.example.banking_system_sdt.customer;

import jakarta.validation.Valid;
import org.example.banking_system_sdt.customer.dto.CustomerResponse;
import org.example.banking_system_sdt.customer.dto.RegisterCustomerRequest;
import org.example.banking_system_sdt.customer.dto.UpdateCustomerRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = customerService.register(
                request.fullName(),
                request.email(),
                request.phone(),
                request.address()
        );
        return CustomerResponse.from(customer);
    }

    @GetMapping("/{id}")
    public CustomerResponse getProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id) {
        Customer customer = customerService.getProfile(callerId, id);
        return CustomerResponse.from(customer);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerService.updateProfile(
                callerId,
                id,
                request.fullName(),
                request.email(),
                request.phone(),
                request.address()
        );
        return CustomerResponse.from(customer);
    }

    @GetMapping
    public List<CustomerResponse> listCustomers(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId) {
        return customerService.listCustomers(callerId).stream()
                .map(CustomerResponse::from)
                .toList();
    }
}
