package com.vahan.oneops.project;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.project.RejectionDtos.RejectionInput;
import com.vahan.oneops.project.RejectionDtos.RejectionRowDto;
import com.vahan.oneops.supplier.Supplier;
import com.vahan.oneops.supplier.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Records pieces a supplier spoiled and what is debited to them, the way Vahan's rejection sheet does.
 * The add/update/delete methods return the id of the project, so the caller can send back the refreshed project.
 */
@Service
public class RejectionService {

    private final RejectionRepository rejections;
    private final PartStageRepository stages;
    private final SupplierRepository suppliers;

    public RejectionService(RejectionRepository rejections, PartStageRepository stages, SupplierRepository suppliers) {
        this.rejections = rejections;
        this.stages = stages;
        this.suppliers = suppliers;
    }

    /** Rejected pieces x part cost - machining cost, never below 0 (same as the sheet). */
    public static BigDecimal calculatedDebit(int qty, BigDecimal partCost, BigDecimal machiningCost) {
        BigDecimal debit = partCost.multiply(BigDecimal.valueOf(qty)).subtract(machiningCost);
        return debit.signum() < 0 ? BigDecimal.ZERO.setScale(2) : debit.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public Long add(Long stageId, RejectionInput in) {
        PartStage stage = stages.findById(stageId).orElseThrow(() -> new NotFoundException("Stage not found"));
        if (stage.getSupplierId() == null) {
            throw new BadRequestException("Give this stage to a supplier before recording a rejection");
        }
        requireWithinSent(stage, in.rejectedQty(), 0);
        Rejection r = new Rejection(stage.getId(), stage.getSupplierId());
        fill(r, in);
        rejections.save(r);
        return stage.getChildPart().getProject().getId();
    }

    @Transactional
    public Long update(Long id, RejectionInput in) {
        Rejection r = find(id);
        PartStage stage = stages.findById(r.getPartStageId()).orElseThrow(() -> new NotFoundException("Stage not found"));
        requireWithinSent(stage, in.rejectedQty(), r.getRejectedQty());
        fill(r, in);
        return stage.getChildPart().getProject().getId();
    }

    @Transactional
    public Long delete(Long id) {
        Rejection r = find(id);
        PartStage stage = stages.findById(r.getPartStageId()).orElseThrow(() -> new NotFoundException("Stage not found"));
        Long projectId = stage.getChildPart().getProject().getId();
        rejections.delete(r);
        return projectId;
    }

    @Transactional(readOnly = true)
    public List<RejectionRowDto> list(Long supplierId) {
        List<Rejection> all = supplierId == null ? rejections.findAllByOrderByIdDesc()
                : rejections.findBySupplierIdOrderByIdDesc(supplierId);
        if (all.isEmpty()) {
            return List.of();
        }
        Map<Long, PartStage> stageById = stages.findAllById(all.stream().map(Rejection::getPartStageId).distinct().toList())
                .stream().collect(Collectors.toMap(PartStage::getId, s -> s));
        Map<Long, String> supplierNames = suppliers.findAllById(all.stream().map(Rejection::getSupplierId).distinct().toList())
                .stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        return all.stream().map(r -> {
            PartStage s = stageById.get(r.getPartStageId());
            ChildPart part = s.getChildPart();
            return new RejectionRowDto(r.getId(), r.getSupplierId(), supplierNames.get(r.getSupplierId()),
                    part.getProject().getId(), part.getProject().getDrawingNo(), part.getDrawingNo(), part.getName(),
                    s.getStageMaster().getName(), r.getRejectedQty(), r.getPartCost(), r.getMachiningCost(),
                    r.getDebitAmount(), r.isDebitManual(), r.getBillNo(), r.getRejectedOn(), r.getRemarks());
        }).toList();
    }

    // ---------- helpers ----------

    /** Total rejected pieces of a stage cannot exceed what was sent (or the part quantity if no count was given). */
    private void requireWithinSent(PartStage stage, int newQty, int oldQty) {
        int sent = stage.getJobQty() != null ? stage.getJobQty() : stage.getChildPart().getQuantity();
        long already = rejections.sumQuantityForStage(stage.getId()) - oldQty;
        if (already + newQty > sent) {
            throw new BadRequestException("Only " + sent + " pieces were sent to the supplier; " + already
                    + " already rejected");
        }
    }

    private void fill(Rejection r, RejectionInput in) {
        BigDecimal machining = in.machiningCost() == null ? BigDecimal.ZERO : in.machiningCost();
        boolean manual = in.debitAmount() != null;
        BigDecimal debit = manual ? in.debitAmount() : calculatedDebit(in.rejectedQty(), in.partCost(), machining);
        r.set(in.rejectedQty(), in.partCost(), machining, debit, manual, blankToNull(in.billNo()), in.rejectedOn(),
                blankToNull(in.remarks()));
    }

    private Rejection find(Long id) {
        return rejections.findById(id).orElseThrow(() -> new NotFoundException("Rejection not found"));
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
