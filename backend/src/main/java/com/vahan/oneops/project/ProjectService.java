package com.vahan.oneops.project;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.project.ProjectDtos.AddPart;
import com.vahan.oneops.project.ProjectDtos.AddStages;
import com.vahan.oneops.project.ProjectDtos.CreateProject;
import com.vahan.oneops.project.ProjectDtos.PartDto;
import com.vahan.oneops.project.ProjectDtos.ProjectDetailDto;
import com.vahan.oneops.project.ProjectDtos.ProjectSummaryDto;
import com.vahan.oneops.project.ProjectDtos.StageDto;
import com.vahan.oneops.project.ProjectDtos.StageInput;
import com.vahan.oneops.project.ProjectDtos.StageMasterDto;
import com.vahan.oneops.project.ProjectDtos.UpdateStage;
import com.vahan.oneops.user.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ProjectService(ProjectRepository projects, ChildPartRepository parts, PartStageRepository stages,
                          StageMasterRepository masters, UserDirectory users) {
        this.projects = projects;
        this.parts = parts;
        this.stages = stages;
        this.masters = masters;
        this.users = users;
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

        Map<Long, List<StageDto>> byPart = allStages.stream().collect(Collectors.groupingBy(
                s -> s.getChildPart().getId(), Collectors.mapping(s -> StageDto.from(s, names), Collectors.toList())));

        List<PartDto> partDtos = new ArrayList<>();
        for (ChildPart p : childParts) {
            partDtos.add(new PartDto(p.getId(), p.getDrawingNo(), p.getName(), p.getQuantity(),
                    byPart.getOrDefault(p.getId(), List.of())));
        }
        return new ProjectDetailDto(project.getId(), project.getDrawingNo(), project.getName(),
                project.getCreatedAt(), partDtos);
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
