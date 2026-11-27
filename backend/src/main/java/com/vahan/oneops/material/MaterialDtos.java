package com.vahan.oneops.material;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class MaterialDtos {

    private MaterialDtos() {
    }

    /** Creates a material, or replaces all of an existing one's details. pricePerKg may be empty until known. */
    public record MaterialInput(
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin(value = "0.1", message = "Density must be at least 0.1 g/cm3")
            @Digits(integer = 3, fraction = 3, message = "Density can have at most 3 decimals") BigDecimal density,
            @DecimalMin(value = "0", message = "Price cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimals") BigDecimal pricePerKg) {
    }

    public record SetActive(boolean active) {
    }

    public record MaterialDto(Long id, String name, BigDecimal density, BigDecimal pricePerKg, boolean active) {
        static MaterialDto from(Material m) {
            return new MaterialDto(m.getId(), m.getName(), m.getDensity(), m.getPricePerKg(), m.isActive());
        }
    }
}
