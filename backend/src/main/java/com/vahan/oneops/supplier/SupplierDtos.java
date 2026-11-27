package com.vahan.oneops.supplier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request and response shapes of the supplier module. */
public final class SupplierDtos {

    private SupplierDtos() {
    }

    // ---------- requests ----------

    /** A stage is picked by name; a name that is not in the stage list yet is added to it. */
    public record ProcessInput(
            @NotBlank @Size(max = 80) String stageName,
            @Size(max = 120) String contactPerson,
            @Size(max = 80) String contactPhone) {
    }

    /** Creates a supplier, or replaces all of an existing supplier's details and processes. */
    public record SupplierInput(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 300) String address,
            @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "The map link must start with http:// or https://") String mapUrl,
            @NotNull @Valid List<ProcessInput> processes) {
    }

    public record SetActive(boolean active) {
    }

    // ---------- responses ----------

    public record ProcessDto(Long id, Long stageMasterId, String stageName, String contactPerson, String contactPhone) {
    }

    /** debitTotal and rejectedQty come from the supplier's rejections; avgTurnaroundDays from jobs sent and received. */
    public record SupplierDto(Long id, String name, String address, String mapUrl, boolean active,
                              List<ProcessDto> processes, java.math.BigDecimal debitTotal, long rejectedQty,
                              Double avgTurnaroundDays, int jobsReceived) {
    }
}
