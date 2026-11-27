package com.vahan.oneops.machine;

import com.vahan.oneops.machine.MachineHourDtos.EntryDto;
import com.vahan.oneops.machine.MachineHourDtos.EntryInput;
import com.vahan.oneops.machine.MachineHourDtos.ListDto;
import com.vahan.oneops.machine.MachineHourDtos.SettingsDto;
import com.vahan.oneops.machine.MachineHourDtos.SettingsInput;
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

/** Admin-only (SecurityConfig protects everything under /api/admin). */
@RestController
@RequestMapping("/api/admin")
public class AdminMachineController {

    private final MachineHourService machineHours;

    public AdminMachineController(MachineHourService machineHours) {
        this.machineHours = machineHours;
    }

    @GetMapping("/machine-hour-settings")
    public SettingsDto settings() {
        return machineHours.getSettings();
    }

    @PutMapping("/machine-hour-settings")
    public SettingsDto updateSettings(@Valid @RequestBody SettingsInput request) {
        return machineHours.updateSettings(request);
    }

    @GetMapping("/machine-hours")
    public ListDto list(@RequestParam(required = false) String month, @RequestParam(required = false) Long projectId) {
        return machineHours.list(month, projectId);
    }

    @PostMapping("/machine-hours")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto add(@Valid @RequestBody EntryInput request) {
        return machineHours.add(request);
    }

    @PutMapping("/machine-hours/{id}")
    public EntryDto update(@PathVariable Long id, @Valid @RequestBody EntryInput request) {
        return machineHours.update(id, request);
    }

    @DeleteMapping("/machine-hours/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        machineHours.delete(id);
    }
}
