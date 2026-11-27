package com.vahan.oneops.tool;

import com.vahan.oneops.tool.ToolDtos.ResharpenInput;
import com.vahan.oneops.tool.ToolDtos.ToolDto;
import com.vahan.oneops.tool.ToolDtos.ToolInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

/** Admin-only (SecurityConfig protects everything under /api/admin). */
@RestController
@RequestMapping("/api/admin/tools")
public class AdminToolController {

    private final ToolService tools;

    public AdminToolController(ToolService tools) {
        this.tools = tools;
    }

    @GetMapping
    public List<ToolDto> list(@RequestParam(required = false) String status) {
        return tools.list(status);
    }

    /** Adds `count` identical tools (default 1) and returns them. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ToolDto> add(@Valid @RequestBody ToolInput request) {
        return tools.add(request);
    }

    @PutMapping("/{id}")
    public ToolDto update(@PathVariable Long id, @Valid @RequestBody ToolInput request) {
        return tools.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tools.delete(id);
    }

    @PostMapping("/{id}/resharpens")
    @ResponseStatus(HttpStatus.CREATED)
    public ToolDto addResharpen(@PathVariable Long id, @Valid @RequestBody ResharpenInput request) {
        return tools.addResharpen(id, request);
    }

    @DeleteMapping("/resharpens/{resharpenId}")
    public ToolDto deleteResharpen(@PathVariable Long resharpenId) {
        return tools.deleteResharpen(resharpenId);
    }
}
