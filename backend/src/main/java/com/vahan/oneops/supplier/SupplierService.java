package com.vahan.oneops.supplier;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.project.StageMaster;
import com.vahan.oneops.project.StageMasterRepository;
import com.vahan.oneops.supplier.SupplierDtos.ProcessDto;
import com.vahan.oneops.supplier.SupplierDtos.ProcessInput;
import com.vahan.oneops.supplier.SupplierDtos.SupplierDto;
import com.vahan.oneops.supplier.SupplierDtos.SupplierInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Admin-side supplier list: add, edit, deactivate. Suppliers are never deleted, old records point at them. */
@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final SupplierProcessRepository processes;
    private final StageMasterRepository masters;

    public SupplierService(SupplierRepository suppliers, SupplierProcessRepository processes,
                           StageMasterRepository masters) {
        this.suppliers = suppliers;
        this.processes = processes;
        this.masters = masters;
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public List<SupplierDto> list(boolean includeInactive) {
        List<Supplier> all = includeInactive ? suppliers.findAll() : suppliers.findByActiveTrue();
        return toDtos(all);
    }

    @Transactional(readOnly = true)
    public SupplierDto get(Long id) {
        return toDtos(List.of(find(id))).get(0);
    }

    // ---------- changing ----------

    @Transactional
    public SupplierDto create(SupplierInput in) {
        String name = tidy(in.name());
        if (suppliers.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("A supplier named '" + name + "' already exists");
        }
        Supplier supplier = suppliers.save(new Supplier(name, blankToNull(in.address()), blankToNull(in.mapUrl())));
        replaceProcesses(supplier, in.processes());
        return toDtos(List.of(supplier)).get(0);
    }

    @Transactional
    public SupplierDto update(Long id, SupplierInput in) {
        Supplier supplier = find(id);
        String name = tidy(in.name());
        suppliers.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("A supplier named '" + name + "' already exists");
            }
        });
        supplier.setName(name);
        supplier.setAddress(blankToNull(in.address()));
        supplier.setMapUrl(blankToNull(in.mapUrl()));
        replaceProcesses(supplier, in.processes());
        return toDtos(List.of(supplier)).get(0);
    }

    @Transactional
    public SupplierDto setActive(Long id, boolean active) {
        Supplier supplier = find(id);
        supplier.setActive(active);
        return toDtos(List.of(supplier)).get(0);
    }

    // ---------- helpers ----------

    /** The processes are saved as a whole list: what is sent is exactly what the supplier has afterwards. */
    private void replaceProcesses(Supplier supplier, List<ProcessInput> inputs) {
        processes.deleteForSupplier(supplier.getId());
        Set<Long> seen = new HashSet<>();
        for (ProcessInput in : inputs) {
            StageMaster stage = findOrCreateStage(in.stageName());
            if (!seen.add(stage.getId())) {
                throw new BadRequestException("'" + stage.getName() + "' is listed twice for this supplier");
            }
            processes.save(new SupplierProcess(supplier, stage, blankToNull(in.contactPerson()),
                    blankToNull(in.contactPhone())));
        }
    }

    /** Same rule as typing a stage into a part's route: match an existing stage by name, or add it to the list. */
    private StageMaster findOrCreateStage(String typed) {
        String name = tidy(typed);
        return masters.findByNameIgnoreCase(name).orElseGet(() -> masters.save(new StageMaster(name)));
    }

    private List<SupplierDto> toDtos(List<Supplier> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(Supplier::getId).toList();
        Map<Long, List<ProcessDto>> byId = processes.findForSuppliers(ids).stream().collect(
                Collectors.groupingBy(p -> p.getSupplier().getId(),
                        Collectors.mapping(p -> new ProcessDto(p.getId(), p.getStageMaster().getId(),
                                p.getStageMaster().getName(), p.getContactPerson(), p.getContactPhone()),
                                Collectors.toList())));
        return list.stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .map(s -> new SupplierDto(s.getId(), s.getName(), s.getAddress(), s.getMapUrl(), s.isActive(),
                        byId.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    private Supplier find(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found"));
    }

    private static String tidy(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
