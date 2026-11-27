package com.vahan.oneops.purchase;

import com.vahan.oneops.purchase.RmPurchaseDtos.ListDto;
import com.vahan.oneops.purchase.RmPurchaseDtos.PurchaseDto;
import com.vahan.oneops.purchase.RmPurchaseDtos.PurchaseInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (SecurityConfig protects everything under /api/admin). */
@RestController
@RequestMapping("/api/admin/rm-purchases")
public class AdminRmPurchaseController {

    private final RmPurchaseService purchases;

    public AdminRmPurchaseController(RmPurchaseService purchases) {
        this.purchases = purchases;
    }

    @GetMapping
    public ListDto list(@RequestParam(required = false) String month,
                        @RequestParam(required = false) Long projectId,
                        @RequestParam(required = false) Long supplierId) {
        return purchases.list(month, projectId, supplierId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDto add(@Valid @RequestBody PurchaseInput request) {
        return purchases.add(request);
    }

    @PutMapping("/{id}")
    public PurchaseDto update(@PathVariable Long id, @Valid @RequestBody PurchaseInput request) {
        return purchases.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        purchases.delete(id);
    }
}
