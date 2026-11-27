package com.vahan.oneops.customer;

import com.vahan.oneops.customer.CustomerDtos.CustomerDto;
import com.vahan.oneops.customer.CustomerDtos.CustomerInput;
import com.vahan.oneops.customer.CustomerDtos.SetActive;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only (SecurityConfig protects everything under /api/admin). */
@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerService customers;

    public AdminCustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    public List<CustomerDto> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return customers.list(includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@Valid @RequestBody CustomerInput request) {
        return customers.create(request);
    }

    @PutMapping("/{id}")
    public CustomerDto rename(@PathVariable Long id, @Valid @RequestBody CustomerInput request) {
        return customers.rename(id, request);
    }

    @PatchMapping("/{id}")
    public CustomerDto setActive(@PathVariable Long id, @RequestBody SetActive request) {
        return customers.setActive(id, request.active());
    }
}
