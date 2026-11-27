package com.vahan.oneops.material;

import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.material.MaterialDtos.MaterialDto;
import com.vahan.oneops.material.MaterialDtos.MaterialInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * The central list of materials. A price change applies to raw material saved from then on;
 * parts keep the price they were costed at.
 */
@Service
public class MaterialService {

    private final MaterialRepository materials;

    public MaterialService(MaterialRepository materials) {
        this.materials = materials;
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> list(boolean includeInactive) {
        return (includeInactive ? materials.findAll() : materials.findByActiveTrue()).stream()
                .sorted(Comparator.comparing(Material::getName, String.CASE_INSENSITIVE_ORDER))
                .map(MaterialDto::from).toList();
    }

    @Transactional
    public MaterialDto create(MaterialInput in) {
        String name = tidy(in.name());
        if (materials.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("A material named '" + name + "' already exists");
        }
        return MaterialDto.from(materials.save(new Material(name, in.density(), in.pricePerKg())));
    }

    @Transactional
    public MaterialDto update(Long id, MaterialInput in) {
        Material m = find(id);
        String name = tidy(in.name());
        materials.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("A material named '" + name + "' already exists");
            }
        });
        m.setName(name);
        m.setDensity(in.density());
        m.setPricePerKg(in.pricePerKg());
        return MaterialDto.from(m);
    }

    @Transactional
    public MaterialDto setActive(Long id, boolean active) {
        Material m = find(id);
        m.setActive(active);
        return MaterialDto.from(m);
    }

    private Material find(Long id) {
        return materials.findById(id).orElseThrow(() -> new NotFoundException("Material not found"));
    }

    private static String tidy(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }
}
