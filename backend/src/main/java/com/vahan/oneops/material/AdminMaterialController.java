package com.vahan.oneops.material;

import com.vahan.oneops.material.MaterialDtos.MaterialDto;
import com.vahan.oneops.material.MaterialDtos.MaterialInput;
import com.vahan.oneops.material.MaterialDtos.SetActive;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/admin/materials")
public class AdminMaterialController {

    private final MaterialService materials;

    public AdminMaterialController(MaterialService materials) {
        this.materials = materials;
    }

    @GetMapping
    public List<MaterialDto> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return materials.list(includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDto create(@Valid @RequestBody MaterialInput request) {
        return materials.create(request);
    }

    @PutMapping("/{id}")
    public MaterialDto update(@PathVariable Long id, @Valid @RequestBody MaterialInput request) {
        return materials.update(id, request);
    }

    @PatchMapping("/{id}")
    public MaterialDto setActive(@PathVariable Long id, @RequestBody SetActive request) {
        return materials.setActive(id, request.active());
    }
}
