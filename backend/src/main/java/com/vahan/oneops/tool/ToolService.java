package com.vahan.oneops.tool;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.project.ChildPart;
import com.vahan.oneops.project.ChildPartRepository;
import com.vahan.oneops.project.Project;
import com.vahan.oneops.project.ProjectRepository;
import com.vahan.oneops.tool.ToolDtos.ResharpenDto;
import com.vahan.oneops.tool.ToolDtos.ResharpenInput;
import com.vahan.oneops.tool.ToolDtos.ToolDto;
import com.vahan.oneops.tool.ToolDtos.ToolInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The tool register: stock, issue, resharpening, breakage and scrapping. */
@Service
public class ToolService {

    private final ToolItemRepository tools;
    private final ToolResharpenRepository resharpens;
    private final ProjectRepository projects;
    private final ChildPartRepository parts;

    public ToolService(ToolItemRepository tools, ToolResharpenRepository resharpens,
                       ProjectRepository projects, ChildPartRepository parts) {
        this.tools = tools;
        this.resharpens = resharpens;
        this.projects = projects;
        this.parts = parts;
    }

    /** status = IN_STOCK / ISSUED / BROKEN / SCRAPPED (optional). Newest first. */
    @Transactional(readOnly = true)
    public List<ToolDto> list(String status) {
        String wanted = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (wanted != null && !Set.of("IN_STOCK", "ISSUED", "BROKEN", "SCRAPPED").contains(wanted)) {
            throw new BadRequestException("Unknown status: " + status);
        }
        List<ToolItem> rows = tools.findAllByOrderByIdDesc().stream()
                .filter(t -> wanted == null || statusOf(t).equals(wanted)).toList();
        return toDtos(rows);
    }

    @Transactional
    public List<ToolDto> add(ToolInput in) {
        int count = in.count() == null ? 1 : in.count();
        List<ToolItem> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ToolItem t = new ToolItem(in.toolType().trim(), in.description().trim());
            fill(t, in);
            created.add(tools.save(t));
        }
        return toDtos(created);
    }

    @Transactional
    public ToolDto update(Long id, ToolInput in) {
        ToolItem t = find(id);
        fill(t, in);
        return toDtos(List.of(t)).get(0);
    }

    /** Only a tool that has never been resharpened can be deleted (the rest is history). */
    @Transactional
    public void delete(Long id) {
        ToolItem t = find(id);
        if (resharpens.existsByToolItemId(id)) {
            throw new ConflictException("This tool has resharpening history, so it can't be deleted");
        }
        tools.delete(t);
    }

    @Transactional
    public ToolDto addResharpen(Long toolId, ResharpenInput in) {
        ToolItem t = find(toolId);
        if (t.getBrokenOn() != null || t.getScrappedOn() != null) {
            throw new BadRequestException("A broken or scrapped tool can't be resharpened");
        }
        resharpens.save(new ToolResharpen(toolId, in.resharpenedOn(), in.cost() == null ? BigDecimal.ZERO : in.cost()));
        return toDtos(List.of(t)).get(0);
    }

    @Transactional
    public ToolDto deleteResharpen(Long resharpenId) {
        ToolResharpen r = resharpens.findById(resharpenId).orElseThrow(() -> new NotFoundException("Resharpening not found"));
        ToolItem t = find(r.getToolItemId());
        resharpens.delete(r);
        resharpens.flush();
        return toDtos(List.of(t)).get(0);
    }

    // ---------- helpers ----------

    private ToolItem find(Long id) {
        return tools.findById(id).orElseThrow(() -> new NotFoundException("Tool not found"));
    }

    private void fill(ToolItem t, ToolInput in) {
        if (in.brokenOn() != null && in.scrappedOn() != null) {
            throw new BadRequestException("A tool is either broken or scrapped, not both");
        }
        boolean hasBrokenLink = in.brokenProjectId() != null || in.brokenPartId() != null
                || (in.brokenReason() != null && !in.brokenReason().isBlank());
        if (hasBrokenLink && in.brokenOn() == null) {
            throw new BadRequestException("Give the date it broke to record the reason, project or part");
        }
        if (in.brokenProjectId() != null) {
            projects.findById(in.brokenProjectId()).orElseThrow(() -> new BadRequestException("Project not found"));
        }
        if (in.brokenPartId() != null) {
            ChildPart part = parts.findById(in.brokenPartId()).orElseThrow(() -> new BadRequestException("Part not found"));
            if (in.brokenProjectId() != null && !part.getProject().getId().equals(in.brokenProjectId())) {
                throw new BadRequestException("That part does not belong to the chosen project");
            }
        }
        if (in.issuedOn() != null && in.purchasedOn() != null && in.issuedOn().isBefore(in.purchasedOn())) {
            throw new BadRequestException("A tool can't be issued before it was bought");
        }
        t.set(in.toolType().trim(), in.description().trim(), blankToNull(in.itemCode()), blankToNull(in.toolSize()),
                in.cost() == null ? BigDecimal.ZERO : in.cost(),
                in.purchasedOn(), in.issuedOn(), in.brokenOn(), in.scrappedOn(),
                blankToNull(in.brokenReason()), in.brokenProjectId(), in.brokenPartId(), blankToNull(in.remarks()));
    }

    static String statusOf(ToolItem t) {
        if (t.getScrappedOn() != null) return "SCRAPPED";
        if (t.getBrokenOn() != null) return "BROKEN";
        if (t.getIssuedOn() != null) return "ISSUED";
        return "IN_STOCK";
    }

    private List<ToolDto> toDtos(Collection<ToolItem> rows) {
        Set<Long> ids = rows.stream().map(ToolItem::getId).collect(Collectors.toSet());
        Map<Long, List<ToolResharpen>> byTool = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ToolResharpen r : resharpens.findByToolItemIdInOrderByIdAsc(ids)) {
                byTool.computeIfAbsent(r.getToolItemId(), k -> new ArrayList<>()).add(r);
            }
        }
        Set<Long> projectIds = new HashSet<>();
        Set<Long> partIds = new HashSet<>();
        for (ToolItem t : rows) {
            if (t.getBrokenProjectId() != null) projectIds.add(t.getBrokenProjectId());
            if (t.getBrokenPartId() != null) partIds.add(t.getBrokenPartId());
        }
        Map<Long, String> projectNo = projects.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Project::getDrawingNo));
        Map<Long, String> partNo = parts.findAllById(partIds).stream()
                .collect(Collectors.toMap(ChildPart::getId, ChildPart::getDrawingNo));
        return rows.stream().map(t -> {
            List<ToolResharpen> rs = byTool.getOrDefault(t.getId(), List.of());
            BigDecimal rsCost = rs.stream().map(ToolResharpen::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new ToolDto(t.getId(), t.getToolType(), t.getDescription(), t.getItemCode(), t.getToolSize(),
                    t.getCost(), t.getPurchasedOn(), t.getIssuedOn(), t.getBrokenOn(), t.getScrappedOn(),
                    t.getBrokenReason(), t.getBrokenProjectId(), projectNo.get(t.getBrokenProjectId()),
                    t.getBrokenPartId(), partNo.get(t.getBrokenPartId()),
                    t.getRemarks(), statusOf(t),
                    rs.stream().map(r -> new ResharpenDto(r.getId(), r.getResharpenedOn(), r.getCost())).toList(),
                    rs.size(), rsCost, t.getCost().add(rsCost));
        }).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
