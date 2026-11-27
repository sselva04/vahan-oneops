package com.vahan.oneops.project;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.project.ProjectDtos.AddPart;
import com.vahan.oneops.project.ProjectDtos.AddStages;
import com.vahan.oneops.project.ProjectDtos.CreateProject;
import com.vahan.oneops.material.Material;
import com.vahan.oneops.material.MaterialRepository;
import com.vahan.oneops.material.WeightCalculator;
import com.vahan.oneops.project.ProjectDtos.OutsourceInput;
import com.vahan.oneops.project.ProjectDtos.RawMaterialDto;
import com.vahan.oneops.project.ProjectDtos.RawMaterialInput;
import com.vahan.oneops.project.ProjectDtos.PartDto;
import com.vahan.oneops.project.ProjectDtos.ProjectDetailDto;
import com.vahan.oneops.project.ProjectDtos.ProjectSummaryDto;
import com.vahan.oneops.project.ProjectDtos.StageDto;
import com.vahan.oneops.project.ProjectDtos.StageInput;
import com.vahan.oneops.project.ProjectDtos.StageMasterDto;
import com.vahan.oneops.project.ProjectDtos.UpdateStage;
import com.vahan.oneops.supplier.Supplier;
import com.vahan.oneops.supplier.SupplierRepository;
import com.vahan.oneops.user.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin-side operations: create projects, add child parts, choose each part's stages and who owns them.
 * Every change returns the full, refreshed project so the screen can simply redraw.
 */
@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final ChildPartRepository parts;
    private final PartStageRepository stages;
    private final StageMasterRepository masters;
    private final UserDirectory users;
    private final SupplierRepository suppliers;
    private final MaterialRepository materials;

    public ProjectService(ProjectRepository projects, ChildPartRepository parts, PartStageRepository stages,
                          StageMasterRepository masters, UserDirectory users, SupplierRepository suppliers,
                          MaterialRepository materials) {
        this.projects = projects;
        this.parts = parts;
        this.stages = stages;
        this.masters = masters;
        this.users = users;
        this.suppliers = suppliers;
        this.materials = materials;
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public List<ProjectSummaryDto> list() {
        List<Project> all = projects.findAllByOrderByCreatedAtDesc();
        if (all.isEmpty()) {
            return List.of();
        }
        List<Long> ids = all.stream().map(Project::getId).toList();
        Map<Long, Long> partCounts = parts.findByProjectIdInOrderByDrawingNoAsc(ids).stream()
                .collect(Collectors.groupingBy(p -> p.getProject().getId(), Collectors.counting()));
        Map<Long, List<PartStage>> stagesByProject = stages.findForProjects(ids).stream()
                .collect(Collectors.groupingBy(s -> s.getChildPart().getProject().getId()));

        return all.stream().map(p -> {
            List<PartStage> ss = stagesByProject.getOrDefault(p.getId(), List.of());
            int done = (int) ss.stream().filter(s -> s.getStatus() == StageStatus.DONE).count();
            int unassigned = (int) ss.stream().filter(s -> s.getAssigneeId() == null).count();
            return new ProjectSummaryDto(p.getId(), p.getDrawingNo(), p.getName(), p.getCreatedAt(),
                    partCounts.getOrDefault(p.getId(), 0L).intValue(), ss.size(), done, unassigned);
        }).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailDto get(Long id) {
        return detail(findProject(id));
    }

    @Transactional(readOnly = true)
    public List<StageMasterDto> stageMasters() {
        return masters.findByActiveTrueOrderByIdAsc().stream().map(StageMasterDto::from).toList();
    }

    // ---------- changing ----------

    @Transactional
    public ProjectDetailDto createProject(Long adminId, CreateProject r) {
        String drawingNo = DrawingNumbers.normalize(r.drawingNo());
        if (projects.existsByDrawingNo(drawingNo)) {
            throw new ConflictException("A project with drawing number " + drawingNo + " already exists");
        }
        Project project = projects.save(new Project(drawingNo, r.name().trim(), adminId));
        return detail(project);
    }

    @Transactional
    public ProjectDetailDto addPart(Long projectId, AddPart r) {
        Project project = findProject(projectId);
        String drawingNo = DrawingNumbers.normalize(r.drawingNo());
        if (parts.existsByDrawingNo(drawingNo)) {
            throw new ConflictException("A part with drawing number " + drawingNo + " already exists");
        }
        parts.save(new ChildPart(project, drawingNo, r.name().trim(), r.quantity()));
        return detail(project);
    }

    /**
     * Sets (or, with no material, clears) the raw material of a part. The price per kg is copied from the
     * material now, so saving again picks up a changed price while untouched parts keep their old one.
     */
    @Transactional
    public ProjectDetailDto updateRawMaterial(Long partId, RawMaterialInput r) {
        ChildPart part = findPart(partId);
        if (r.materialId() == null) {
            if (r.shape() != null || r.dimA() != null || r.dimB() != null || r.dimC() != null
                    || r.manualWeightKg() != null || r.actualWeightKg() != null) {
                throw new BadRequestException("Choose a material first");
            }
            part.setRawMaterial(null, null, null, null, null, null, null, null);
            return detail(part.getProject());
        }
        Material material = materials.findById(r.materialId())
                .orElseThrow(() -> new BadRequestException("Unknown material"));
        if (!material.isActive() && !material.getId().equals(part.getMaterialId())) {
            throw new BadRequestException("This material is deactivated");
        }
        if (r.shape() == null) {
            throw new BadRequestException("Choose a shape");
        }
        requireSizes(r);
        part.setRawMaterial(material.getId(), r.shape(), r.dimA(), r.dimB(), r.dimC(), r.manualWeightKg(),
                r.actualWeightKg(), material.getPricePerKg());
        return detail(part.getProject());
    }

    /** Each shape needs exactly its own sizes; anything else is rejected so no stale value is kept. */
    private static void requireSizes(RawMaterialInput r) {
        boolean a = r.dimA() != null, b = r.dimB() != null, c = r.dimC() != null, m = r.manualWeightKg() != null;
        switch (r.shape()) {
            case BLOCK -> {
                if (!(a && b && c) || m) {
                    throw new BadRequestException("A block needs length, width and thickness");
                }
            }
            case ROUND -> {
                if (!(a && b) || c || m) {
                    throw new BadRequestException("A round bar needs diameter and length");
                }
            }
            case TUBE -> {
                if (!(a && b && c) || m) {
                    throw new BadRequestException("A tube needs outer diameter, inner diameter and length");
                }
                if (r.dimB().compareTo(r.dimA()) >= 0) {
                    throw new BadRequestException("The inner diameter must be smaller than the outer diameter");
                }
            }
            case MANUAL -> {
                if (a || b || c || !m) {
                    throw new BadRequestException("Enter only the weight per piece");
                }
            }
        }
    }

    private RawMaterialDto rawMaterial(ChildPart p, Map<Long, Material> byId) {
        Material m = p.getMaterialId() == null ? null : byId.get(p.getMaterialId());
        if (m == null || p.getRmShape() == null) {
            return null;
        }
        BigDecimal perPiece = WeightCalculator.perPieceKg(p.getRmShape(), m.getDensity(),
                p.getRmDimA(), p.getRmDimB(), p.getRmDimC(), p.getManualWeightKg());
        BigDecimal theoretical = WeightCalculator.totalKg(perPiece, p.getQuantity());
        BigDecimal actual = p.getActualWeightKg();
        BigDecimal diff = null;
        BigDecimal diffPct = null;
        if (actual != null) {
            diff = actual.subtract(theoretical);
            if (theoretical.signum() > 0) {
                diffPct = diff.multiply(BigDecimal.valueOf(100)).divide(theoretical, 1, RoundingMode.HALF_UP);
            }
        }
        BigDecimal rate = p.getRmRatePerKg();
        BigDecimal basis = actual != null ? actual : theoretical;
        BigDecimal cost = rate == null ? null : basis.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new RawMaterialDto(m.getId(), m.getName(), p.getRmShape(), p.getRmDimA(), p.getRmDimB(),
                p.getRmDimC(), p.getManualWeightKg(), perPiece, theoretical, actual, diff, diffPct, rate, cost,
                actual != null);
    }

    @Transactional
    public ProjectDetailDto addStages(Long partId, AddStages r) {
        ChildPart part = findPart(partId);
        int sequence = stages.maxSequence(partId);
        for (StageInput in : r.stages()) {
            StageMaster master = resolveStage(in);
            requireActiveAssignee(in.assigneeId());
            stages.save(new PartStage(part, master, ++sequence, in.assigneeId(), in.dueDate()));
        }
        return detail(part.getProject());
    }

    @Transactional
    public ProjectDetailDto updateStage(Long stageId, UpdateStage r) {
        PartStage stage = findStage(stageId);
        requireActiveAssignee(r.assigneeId());
        stage.setAssigneeId(r.assigneeId());
        stage.setDueDate(r.dueDate());
        return detail(stage.getChildPart().getProject());
    }

    /**
     * Gives a stage to a supplier (or takes it back in-house) and records the job details:
     * pieces sent, rate per piece, inspections and remarks.
     */
    @Transactional
    public ProjectDetailDto updateOutsourcing(Long stageId, OutsourceInput r) {
        PartStage stage = findStage(stageId);
        Long supplierId = r.supplierId();
        if (supplierId == null) {
            if (r.jobQty() != null || r.ratePerUnit() != null) {
                throw new BadRequestException("Quantity and rate are only for a stage given to a supplier");
            }
        } else {
            Supplier supplier = suppliers.findById(supplierId)
                    .orElseThrow(() -> new BadRequestException("Unknown supplier"));
            // A supplier deactivated since this stage was given to them may stay on it; it just cannot be newly chosen.
            if (!supplier.isActive() && !supplierId.equals(stage.getSupplierId())) {
                throw new BadRequestException("This supplier is deactivated");
            }
        }
        stage.setSupplierId(supplierId);
        stage.setJobQty(r.jobQty());
        stage.setRatePerUnit(r.ratePerUnit());
        stage.setPreInspectionDone(r.preInspectionDone());
        stage.setFinalInspectionDone(r.finalInspectionDone());
        String remarks = r.remarks() == null ? null : r.remarks().trim();
        stage.setRemarks(remarks == null || remarks.isEmpty() ? null : remarks);
        return detail(stage.getChildPart().getProject());
    }

    @Transactional
    public ProjectDetailDto deleteStage(Long stageId) {
        PartStage stage = findStage(stageId);
        if (stage.getStatus() != StageStatus.NOT_STARTED) {
            throw new ConflictException("Only stages that have not started can be removed");
        }
        Project project = stage.getChildPart().getProject();
        stages.delete(stage);
        stages.flush();
        return detail(project);
    }

    @Transactional
    public StageMasterDto createStageMaster(String name) {
        String clean = name.trim();
        if (masters.existsByNameIgnoreCase(clean)) {
            throw new ConflictException("Stage '" + clean + "' already exists");
        }
        return StageMasterDto.from(masters.save(new StageMaster(clean)));
    }

    // ---------- helpers ----------

    private ProjectDetailDto detail(Project project) {
        List<ChildPart> childParts = parts.findByProjectIdInOrderByDrawingNoAsc(List.of(project.getId()));
        List<Long> partIds = childParts.stream().map(ChildPart::getId).toList();
        List<PartStage> allStages = partIds.isEmpty() ? List.of() : stages.findForParts(partIds);

        Set<Long> assigneeIds = new HashSet<>();
        allStages.forEach(s -> {
            if (s.getAssigneeId() != null) {
                assigneeIds.add(s.getAssigneeId());
            }
        });
        Map<Long, String> names = users.displayNames(assigneeIds);

        Set<Long> supplierIds = new HashSet<>();
        allStages.forEach(s -> {
            if (s.getSupplierId() != null) {
                supplierIds.add(s.getSupplierId());
            }
        });
        Map<Long, String> supplierNames = supplierIds.isEmpty() ? Map.of()
                : suppliers.findAllById(supplierIds).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName));

        Map<Long, List<StageDto>> byPart = allStages.stream().collect(Collectors.groupingBy(
                s -> s.getChildPart().getId(),
                Collectors.mapping(s -> StageDto.from(s, names, supplierNames), Collectors.toList())));

        Set<Long> materialIds = new HashSet<>();
        childParts.forEach(p -> {
            if (p.getMaterialId() != null) {
                materialIds.add(p.getMaterialId());
            }
        });
        Map<Long, Material> materialById = materialIds.isEmpty() ? Map.of()
                : materials.findAllById(materialIds).stream().collect(Collectors.toMap(Material::getId, m -> m));

        List<PartDto> partDtos = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ChildPart p : childParts) {
            RawMaterialDto rm = rawMaterial(p, materialById);
            if (rm != null && rm.cost() != null) {
                totalCost = totalCost.add(rm.cost());
            }
            partDtos.add(new PartDto(p.getId(), p.getDrawingNo(), p.getName(), p.getQuantity(),
                    byPart.getOrDefault(p.getId(), List.of()), rm));
        }
        return new ProjectDetailDto(project.getId(), project.getDrawingNo(), project.getName(),
                project.getCreatedAt(), partDtos, totalCost);
    }

    /**
     * A stage is either picked from the list or typed in. A typed name is matched to an existing stage
     * (ignoring case and extra spaces), otherwise it is added to the list so it is there next time.
     */
    private StageMaster resolveStage(StageInput in) {
        boolean hasCustom = in.customName() != null && !in.customName().isBlank();
        if (hasCustom && in.stageMasterId() != null) {
            throw new BadRequestException("Choose a stage from the list or type a custom one, not both");
        }
        if (hasCustom) {
            String name = in.customName().trim().replaceAll("\\s+", " ");
            return masters.findByNameIgnoreCase(name).orElseGet(() -> masters.save(new StageMaster(name)));
        }
        if (in.stageMasterId() == null) {
            throw new BadRequestException("Choose a stage or type a custom one");
        }
        return masters.findById(in.stageMasterId()).filter(StageMaster::isActive)
                .orElseThrow(() -> new BadRequestException("Unknown stage"));
    }

    private void requireActiveAssignee(Long assigneeId) {
        if (assigneeId != null && !users.isActive(assigneeId)) {
            throw new BadRequestException("The person must be an active user");
        }
    }

    private Project findProject(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private ChildPart findPart(Long id) {
        return parts.findById(id).orElseThrow(() -> new NotFoundException("Part not found"));
    }

    private PartStage findStage(Long id) {
        return stages.findById(id).orElseThrow(() -> new NotFoundException("Stage not found"));
    }
}
