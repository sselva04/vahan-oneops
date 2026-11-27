package com.vahan.oneops.project;

import com.vahan.oneops.common.AuthUser;
import com.vahan.oneops.project.ProjectDtos.AddPart;
import com.vahan.oneops.project.ProjectDtos.AddStages;
import com.vahan.oneops.project.ProjectDtos.CreateProject;
import com.vahan.oneops.project.ProjectDtos.CreateStageMaster;
import com.vahan.oneops.project.ProjectDtos.OutsourceInput;
import com.vahan.oneops.project.ProjectDtos.RawMaterialInput;
import com.vahan.oneops.project.ProjectDtos.ProjectDetailDto;
import com.vahan.oneops.project.ProjectDtos.ProjectSummaryDto;
import com.vahan.oneops.project.ProjectDtos.SetCustomer;
import com.vahan.oneops.project.ProjectDtos.StageMasterDto;
import com.vahan.oneops.project.RejectionDtos.RejectionInput;
import com.vahan.oneops.project.RejectionDtos.RejectionRowDto;
import com.vahan.oneops.project.ProjectDtos.UpdateStage;
import com.vahan.oneops.project.ProjectDtos.WorkItemDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.List;

/** Admin-only (enforced by SecurityConfig for everything under /api/admin). */
@RestController
@RequestMapping("/api/admin")
public class AdminProjectController {

    private final ProjectService projects;
    private final WorkService work;
    private final RejectionService rejections;

    public AdminProjectController(ProjectService projects, WorkService work, RejectionService rejections) {
        this.projects = projects;
        this.work = work;
        this.rejections = rejections;
    }

    @GetMapping("/projects")
    public List<ProjectSummaryDto> list() {
        return projects.list();
    }

    @GetMapping("/projects/{id}")
    public ProjectDetailDto get(@PathVariable Long id) {
        return projects.get(id);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateProject request) {
        return projects.createProject(me.id(), request);
    }

    @PutMapping("/projects/{id}/customer")
    public ProjectDetailDto setCustomer(@PathVariable Long id, @Valid @RequestBody SetCustomer request) {
        return projects.setCustomer(id, request.customerId());
    }

    @PostMapping("/projects/{id}/parts")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailDto addPart(@PathVariable Long id, @Valid @RequestBody AddPart request) {
        return projects.addPart(id, request);
    }

    @PostMapping("/parts/{id}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailDto addStages(@PathVariable Long id, @Valid @RequestBody AddStages request) {
        return projects.addStages(id, request);
    }

    @PutMapping("/stages/{id}")
    public ProjectDetailDto updateStage(@PathVariable Long id, @Valid @RequestBody UpdateStage request) {
        return projects.updateStage(id, request);
    }

    /** Give a stage to a supplier (or back to in-house) and record the job details. */
    /** Set or clear the raw material of a part (material, shape, size, actual weight). */
    @PutMapping("/parts/{id}/raw-material")
    public ProjectDetailDto updateRawMaterial(@PathVariable Long id, @Valid @RequestBody RawMaterialInput request) {
        return projects.updateRawMaterial(id, request);
    }

    // ---- supplier rejections (each call returns the refreshed project) ----

    @PostMapping("/stages/{id}/rejections")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailDto addRejection(@PathVariable Long id, @Valid @RequestBody RejectionInput request) {
        return projects.get(rejections.add(id, request));
    }

    @PutMapping("/rejections/{id}")
    public ProjectDetailDto updateRejection(@PathVariable Long id, @Valid @RequestBody RejectionInput request) {
        return projects.get(rejections.update(id, request));
    }

    @DeleteMapping("/rejections/{id}")
    public ProjectDetailDto deleteRejection(@PathVariable Long id) {
        return projects.get(rejections.delete(id));
    }

    /** All rejections, newest first; optionally only one supplier's. */
    @GetMapping("/rejections")
    public List<RejectionRowDto> listRejections(@RequestParam(required = false) Long supplierId) {
        return rejections.list(supplierId);
    }

    @PutMapping("/stages/{id}/outsourcing")
    public ProjectDetailDto updateOutsourcing(@PathVariable Long id, @Valid @RequestBody OutsourceInput request) {
        return projects.updateOutsourcing(id, request);
    }

    @DeleteMapping("/stages/{id}")
    public ProjectDetailDto deleteStage(@PathVariable Long id) {
        return projects.deleteStage(id);
    }

    @PostMapping("/stage-master")
    @ResponseStatus(HttpStatus.CREATED)
    public StageMasterDto createStageMaster(@Valid @RequestBody CreateStageMaster request) {
        return projects.createStageMaster(request.name());
    }

    /** The admin's view of one person's allocated work. */
    @GetMapping("/users/{id}/work")
    public List<WorkItemDto> userWork(@PathVariable Long id) {
        return work.forUserAsAdmin(id);
    }
}
