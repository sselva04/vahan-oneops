package com.vahan.oneops.project;

import com.vahan.oneops.common.AuthUser;
import com.vahan.oneops.project.ProjectDtos.StageMasterDto;
import com.vahan.oneops.project.ProjectDtos.WorkItemDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints for any logged-in user. */
@RestController
public class WorkController {

    private final WorkService work;
    private final ProjectService projects;

    public WorkController(WorkService work, ProjectService projects) {
        this.work = work;
        this.projects = projects;
    }

    /** Only ever returns the caller's own work: the user id comes from the token, never from the request. */
    @GetMapping("/api/my-work")
    public List<WorkItemDto> myWork(@AuthenticationPrincipal AuthUser me) {
        return work.forUser(me.id());
    }

    /** Start one of my stages. Returns my refreshed list. */
    @PostMapping("/api/my-work/{stageId}/start")
    public List<WorkItemDto> start(@AuthenticationPrincipal AuthUser me, @PathVariable Long stageId) {
        return work.start(me.id(), stageId);
    }

    /** Mark one of my started stages as done. Returns my refreshed list. */
    @PostMapping("/api/my-work/{stageId}/complete")
    public List<WorkItemDto> complete(@AuthenticationPrincipal AuthUser me, @PathVariable Long stageId) {
        return work.complete(me.id(), stageId);
    }

    @GetMapping("/api/stage-master")
    public List<StageMasterDto> stageMasters() {
        return projects.stageMasters();
    }
}
