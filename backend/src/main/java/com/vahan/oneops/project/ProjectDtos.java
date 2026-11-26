package com.vahan.oneops.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response shapes of the project module, grouped in one file. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    // ---------- requests ----------

    public record CreateProject(
            @NotBlank @Size(max = 60) String drawingNo,
            @NotBlank @Size(max = 200) String name) {
    }

    public record AddPart(
            @NotBlank @Size(max = 60) String drawingNo,
            @NotBlank @Size(max = 200) String name,
            @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
    }

    /** Give either stageMasterId (a stage from the list) or customName (a stage typed in), not both. */
    public record StageInput(Long stageMasterId, @Size(max = 80) String customName,
                             Long assigneeId, LocalDate dueDate) {
    }

    /** The stages are appended to the part in the order given. */
    public record AddStages(@NotEmpty @Valid List<StageInput> stages) {
    }

    /** Replaces both values: assigneeId = null means "unassigned", dueDate = null means "no due date". */
    public record UpdateStage(Long assigneeId, LocalDate dueDate) {
    }

    public record CreateStageMaster(@NotBlank @Size(max = 80) String name) {
    }

    // ---------- responses ----------

    public record StageMasterDto(Long id, String name) {
        static StageMasterDto from(StageMaster m) {
            return new StageMasterDto(m.getId(), m.getName());
        }
    }

    public record StageDto(Long id, Long stageMasterId, String stageName, int sequenceNo,
                           Long assigneeId, String assigneeName, StageStatus status, LocalDate dueDate,
                           Instant startedAt, Instant completedAt) {
        static StageDto from(PartStage s, Map<Long, String> names) {
            Long assignee = s.getAssigneeId();
            return new StageDto(s.getId(), s.getStageMaster().getId(), s.getStageMaster().getName(),
                    s.getSequenceNo(), assignee, assignee == null ? null : names.get(assignee),
                    s.getStatus(), s.getDueDate(), s.getStartedAt(), s.getCompletedAt());
        }
    }

    public record PartDto(Long id, String drawingNo, String name, int quantity, List<StageDto> stages) {
    }

    public record ProjectDetailDto(Long id, String drawingNo, String name, Instant createdAt, List<PartDto> parts) {
    }

    public record ProjectSummaryDto(Long id, String drawingNo, String name, Instant createdAt,
                                    int partCount, int stageCount, int stagesDone, int stagesUnassigned) {
    }

    /** One row of "My work": a stage allocated to a person, with enough context to show it on its own. */
    public record WorkItemDto(Long stageId, Long projectId, String projectDrawingNo, String projectName,
                              Long partId, String partDrawingNo, String partName, int quantity,
                              String stageName, int sequenceNo, StageStatus status, LocalDate dueDate,
                              Instant startedAt, Instant completedAt, List<String> waitingFor) {
    }
}
