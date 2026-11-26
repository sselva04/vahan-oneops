package com.vahan.oneops.project;

import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.ForbiddenException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.project.ProjectDtos.WorkItemDto;
import com.vahan.oneops.user.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "My work": the stages allocated to one person, and the two things an employee does with them:
 * start a stage, and mark it done. Used by the employee screen and the admin's per-user view.
 */
@Service
public class WorkService {

    private final PartStageRepository stages;
    private final UserDirectory users;

    public WorkService(PartStageRepository stages, UserDirectory users) {
        this.stages = stages;
        this.users = users;
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public List<WorkItemDto> forUser(Long userId) {
        List<PartStage> mine = stages.findAssignedTo(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Set<Long> partIds = mine.stream().map(s -> s.getChildPart().getId()).collect(Collectors.toSet());
        // All stages of those parts, so we can tell which earlier stages are still unfinished.
        Map<Long, List<PartStage>> byPart = stages.findForParts(partIds).stream()
                .collect(Collectors.groupingBy(s -> s.getChildPart().getId()));

        return mine.stream()
                .sorted(Comparator
                        .comparing((PartStage s) -> s.getStatus() == StageStatus.DONE)   // finished work last
                        .thenComparing(PartStage::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(s -> s.getChildPart().getDrawingNo())
                        .thenComparingInt(PartStage::getSequenceNo))
                .map(s -> {
                    ChildPart part = s.getChildPart();
                    Project project = part.getProject();
                    return new WorkItemDto(s.getId(), project.getId(), project.getDrawingNo(), project.getName(),
                            part.getId(), part.getDrawingNo(), part.getName(), part.getQuantity(),
                            s.getStageMaster().getName(), s.getSequenceNo(), s.getStatus(), s.getDueDate(),
                            s.getStartedAt(), s.getCompletedAt(), waitingFor(byPart.get(part.getId()), s));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkItemDto> forUserAsAdmin(Long userId) {
        if (users.displayNames(List.of(userId)).isEmpty()) {
            throw new NotFoundException("User not found");
        }
        return forUser(userId);
    }

    // ---------- doing the work ----------

    /** Rule: a stage can start only when every earlier stage of the same part is done. */
    @Transactional
    public List<WorkItemDto> start(Long userId, Long stageId) {
        PartStage stage = ownedBy(userId, stageId);
        if (stage.getStatus() != StageStatus.NOT_STARTED) {
            throw new ConflictException("This stage has already been started");
        }
        List<String> waiting = waitingFor(stages.findForParts(List.of(stage.getChildPart().getId())), stage);
        if (!waiting.isEmpty()) {
            throw new ConflictException("Waiting for: " + String.join(", ", waiting));
        }
        stage.start();
        return forUser(userId);
    }

    /** Rule: only a started stage can be marked done. */
    @Transactional
    public List<WorkItemDto> complete(Long userId, Long stageId) {
        PartStage stage = ownedBy(userId, stageId);
        if (stage.getStatus() == StageStatus.DONE) {
            throw new ConflictException("This stage is already done");
        }
        if (stage.getStatus() != StageStatus.IN_PROGRESS) {
            throw new ConflictException("Start the stage before marking it done");
        }
        stage.complete(userId);
        return forUser(userId);
    }

    // ---------- helpers ----------

    /** Only the person the stage is allocated to may start or finish it (admins included). */
    private PartStage ownedBy(Long userId, Long stageId) {
        PartStage stage = stages.findById(stageId).orElseThrow(() -> new NotFoundException("Stage not found"));
        if (!userId.equals(stage.getAssigneeId())) {
            throw new ForbiddenException("This stage is not allocated to you");
        }
        return stage;
    }

    private static List<String> waitingFor(List<PartStage> allStagesOfPart, PartStage stage) {
        return allStagesOfPart.stream()
                .filter(o -> o.getSequenceNo() < stage.getSequenceNo() && o.getStatus() != StageStatus.DONE)
                .map(o -> o.getStageMaster().getName())
                .toList();
    }
}
