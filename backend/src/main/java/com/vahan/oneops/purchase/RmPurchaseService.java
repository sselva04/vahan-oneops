package com.vahan.oneops.purchase;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.customer.Customer;
import com.vahan.oneops.customer.CustomerRepository;
import com.vahan.oneops.material.Material;
import com.vahan.oneops.material.MaterialRepository;
import com.vahan.oneops.project.ChildPart;
import com.vahan.oneops.project.ChildPartRepository;
import com.vahan.oneops.project.Project;
import com.vahan.oneops.purchase.RmPurchaseDtos.ListDto;
import com.vahan.oneops.purchase.RmPurchaseDtos.PurchaseDto;
import com.vahan.oneops.purchase.RmPurchaseDtos.PurchaseInput;
import com.vahan.oneops.purchase.RmPurchaseDtos.Totals;
import com.vahan.oneops.supplier.Supplier;
import com.vahan.oneops.supplier.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The raw material purchase log: what was bought for which part, from whom, at what rate. */
@Service
public class RmPurchaseService {

    private final RmPurchaseRepository purchases;
    private final ChildPartRepository parts;
    private final SupplierRepository suppliers;
    private final MaterialRepository materials;
    private final CustomerRepository customers;

    public RmPurchaseService(RmPurchaseRepository purchases, ChildPartRepository parts, SupplierRepository suppliers,
                             MaterialRepository materials, CustomerRepository customers) {
        this.purchases = purchases;
        this.parts = parts;
        this.suppliers = suppliers;
        this.materials = materials;
        this.customers = customers;
    }

    /** Total = weight x rate per kg, 2 decimals. */
    public static BigDecimal total(BigDecimal weightKg, BigDecimal ratePerKg) {
        return weightKg.multiply(ratePerKg).setScale(2, RoundingMode.HALF_UP);
    }

    /** month = "2026-11", projectId and supplierId all optional. Newest first. */
    @Transactional(readOnly = true)
    public ListDto list(String month, Long projectId, Long supplierId) {
        YearMonth ym = null;
        if (month != null && !month.isBlank()) {
            try {
                ym = YearMonth.parse(month.trim());
            } catch (RuntimeException e) {
                throw new BadRequestException("Month must look like 2026-11");
            }
        }
        final YearMonth wanted = ym;
        List<RmPurchase> rows = purchases.findAllByOrderByInDateDescIdDesc().stream()
                .filter(p -> wanted == null || YearMonth.from(p.getInDate()).equals(wanted))
                .filter(p -> supplierId == null || supplierId.equals(p.getSupplierId()))
                .toList();
        List<PurchaseDto> dtos = toDtos(rows);
        if (projectId != null) {
            dtos = dtos.stream().filter(d -> projectId.equals(d.projectId())).toList();
        }
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (PurchaseDto d : dtos) {
            weight = weight.add(d.weightKg());
            cost = cost.add(d.totalCost());
        }
        return new ListDto(dtos, new Totals(weight, cost));
    }

    @Transactional
    public PurchaseDto add(PurchaseInput in) {
        RmPurchase p = new RmPurchase(in.partId(), in.supplierId());
        fill(p, in);
        return toDtos(List.of(purchases.save(p))).get(0);
    }

    @Transactional
    public PurchaseDto update(Long id, PurchaseInput in) {
        RmPurchase p = purchases.findById(id).orElseThrow(() -> new NotFoundException("Purchase not found"));
        fill(p, in);
        return toDtos(List.of(p)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        purchases.delete(purchases.findById(id).orElseThrow(() -> new NotFoundException("Purchase not found")));
    }

    private void fill(RmPurchase p, PurchaseInput in) {
        parts.findById(in.partId()).orElseThrow(() -> new BadRequestException("Part not found"));
        suppliers.findById(in.supplierId()).orElseThrow(() -> new BadRequestException("Supplier not found"));
        if (in.outDate() != null && in.outDate().isBefore(in.inDate())) {
            throw new BadRequestException("The out date can't be before the in date");
        }
        String remarks = in.remarks() == null || in.remarks().isBlank() ? null : in.remarks().trim();
        p.set(in.partId(), in.supplierId(), in.weightKg().setScale(3, RoundingMode.HALF_UP),
                in.ratePerKg().setScale(2, RoundingMode.HALF_UP), total(in.weightKg(), in.ratePerKg()),
                in.inDate(), in.outDate(), remarks);
    }

    private List<PurchaseDto> toDtos(List<RmPurchase> rows) {
        Set<Long> partIds = rows.stream().map(RmPurchase::getChildPartId).collect(Collectors.toSet());
        Set<Long> supplierIds = rows.stream().map(RmPurchase::getSupplierId).collect(Collectors.toSet());
        Map<Long, ChildPart> partById = parts.findAllById(partIds).stream()
                .collect(Collectors.toMap(ChildPart::getId, Function.identity()));
        Map<Long, String> supplierName = suppliers.findAllById(supplierIds).stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        Set<Long> materialIds = partById.values().stream().map(ChildPart::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> materialName = materials.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));
        Set<Long> customerIds = partById.values().stream().map(c -> c.getProject().getCustomerId())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerName = customers.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        return rows.stream().map(p -> {
            ChildPart part = partById.get(p.getChildPartId());
            Project project = part == null ? null : part.getProject();
            return new PurchaseDto(p.getId(), p.getChildPartId(),
                    part == null ? null : part.getDrawingNo(), part == null ? null : part.getName(),
                    project == null ? null : project.getId(), project == null ? null : project.getDrawingNo(),
                    project == null || project.getCustomerId() == null ? null : customerName.get(project.getCustomerId()),
                    part == null || part.getMaterialId() == null ? null : materialName.get(part.getMaterialId()),
                    p.getSupplierId(), supplierName.get(p.getSupplierId()),
                    p.getWeightKg(), p.getRatePerKg(), p.getTotalCost(), p.getInDate(), p.getOutDate(), p.getRemarks());
        }).toList();
    }
}
