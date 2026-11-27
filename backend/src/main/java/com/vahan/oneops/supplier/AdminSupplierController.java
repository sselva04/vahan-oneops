package com.vahan.oneops.supplier;

import com.vahan.oneops.supplier.SupplierDtos.SetActive;
import com.vahan.oneops.supplier.SupplierDtos.SupplierDto;
import com.vahan.oneops.supplier.SupplierDtos.SupplierInput;
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

/** Admin-only (enforced by SecurityConfig for everything under /api/admin). */
@RestController
@RequestMapping("/api/admin/suppliers")
public class AdminSupplierController {

    private final SupplierService suppliers;

    public AdminSupplierController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping
    public List<SupplierDto> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return suppliers.list(includeInactive);
    }

    @GetMapping("/{id}")
    public SupplierDto get(@PathVariable Long id) {
        return suppliers.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto create(@Valid @RequestBody SupplierInput request) {
        return suppliers.create(request);
    }

    @PutMapping("/{id}")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierInput request) {
        return suppliers.update(id, request);
    }

    @PatchMapping("/{id}")
    public SupplierDto setActive(@PathVariable Long id, @RequestBody SetActive request) {
        return suppliers.setActive(id, request.active());
    }
}
