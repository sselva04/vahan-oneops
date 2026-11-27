package com.vahan.oneops.customer;

import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.customer.CustomerDtos.CustomerDto;
import com.vahan.oneops.customer.CustomerDtos.CustomerInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** The central customer list. Customers are never deleted: projects point at them. */
@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> list(boolean includeInactive) {
        return (includeInactive ? customers.findAll() : customers.findByActiveTrue()).stream()
                .sorted(Comparator.comparing(Customer::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CustomerDto::from).toList();
    }

    @Transactional
    public CustomerDto create(CustomerInput in) {
        String name = tidy(in.name());
        if (customers.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("A customer named '" + name + "' already exists");
        }
        return CustomerDto.from(customers.save(new Customer(name)));
    }

    @Transactional
    public CustomerDto rename(Long id, CustomerInput in) {
        Customer c = find(id);
        String name = tidy(in.name());
        customers.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("A customer named '" + name + "' already exists");
            }
        });
        c.setName(name);
        return CustomerDto.from(c);
    }

    @Transactional
    public CustomerDto setActive(Long id, boolean active) {
        Customer c = find(id);
        c.setActive(active);
        return CustomerDto.from(c);
    }

    private Customer find(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private static String tidy(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }
}
