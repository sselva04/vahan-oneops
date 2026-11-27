package com.vahan.oneops.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerInput(@NotBlank @Size(max = 120) String name) {
    }

    public record SetActive(boolean active) {
    }

    public record CustomerDto(Long id, String name, boolean active) {
        static CustomerDto from(Customer c) {
            return new CustomerDto(c.getId(), c.getName(), c.isActive());
        }
    }
}
