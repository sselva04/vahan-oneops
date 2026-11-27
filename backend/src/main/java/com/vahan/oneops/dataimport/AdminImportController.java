package com.vahan.oneops.dataimport;

import com.vahan.oneops.common.AuthUser;
import com.vahan.oneops.dataimport.ImportDtos.ExpenseItem;
import com.vahan.oneops.dataimport.ImportDtos.HourItem;
import com.vahan.oneops.dataimport.ImportDtos.PartItem;
import com.vahan.oneops.dataimport.ImportDtos.ProjectItem;
import com.vahan.oneops.dataimport.ImportDtos.PurchaseItem;
import com.vahan.oneops.dataimport.ImportDtos.RejectionItem;
import com.vahan.oneops.dataimport.ImportDtos.SectionResult;
import com.vahan.oneops.dataimport.ImportDtos.StageItem;
import com.vahan.oneops.dataimport.ImportDtos.Status;
import com.vahan.oneops.dataimport.ImportDtos.ToolImportItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Dev-only import endpoints. The controller only exists when vahan.import.enabled=true, so everywhere else these
 * URLs simply do not exist. Admin-only like everything under /api/admin.
 */
@RestController
@RequestMapping("/api/admin/import")
@ConditionalOnProperty(name = "vahan.import.enabled", havingValue = "true")
public class AdminImportController {

    private final ImportService importer;

    public AdminImportController(ImportService importer) {
        this.importer = importer;
    }

    @GetMapping("/status")
    public Status status() {
        return new Status(true, importer.batches());
    }

    @PostMapping("/begin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void begin(@RequestParam String batch) {
        importer.begin(batch);
    }

    @DeleteMapping("/batches/{batch}")
    public Map<String, Integer> remove(@PathVariable String batch) {
        return importer.remove(batch);
    }

    @PostMapping("/customers")
    public SectionResult customers(@RequestBody List<String> names) {
        return importer.customers(names);
    }

    @PostMapping("/suppliers")
    public SectionResult suppliers(@RequestParam String batch, @RequestBody List<String> names) {
        return importer.suppliers(batch, names);
    }

    @PostMapping("/projects")
    public SectionResult projects(@RequestParam String batch, @AuthenticationPrincipal AuthUser me,
                                  @RequestBody List<ProjectItem> items) {
        return importer.projects(batch, me.id(), items);
    }

    @PostMapping("/parts")
    public SectionResult parts(@RequestParam String batch, @RequestBody List<PartItem> items) {
        return importer.parts(batch, items);
    }

    @PostMapping("/stages")
    public SectionResult stages(@RequestParam String batch, @AuthenticationPrincipal AuthUser me,
                                @RequestBody List<StageItem> items) {
        return importer.stages(batch, me.id(), items);
    }

    @PostMapping("/machine-hours")
    public SectionResult machineHours(@RequestParam String batch, @RequestBody List<HourItem> items) {
        return importer.machineHours(batch, items);
    }

    @PostMapping("/tools")
    public SectionResult tools(@RequestParam String batch, @RequestBody List<ToolImportItem> items) {
        return importer.tools(batch, items);
    }

    @PostMapping("/rm-purchases")
    public SectionResult rmPurchases(@RequestParam String batch, @RequestBody List<PurchaseItem> items) {
        return importer.rmPurchases(batch, items);
    }

    @PostMapping("/rejections")
    public SectionResult rejections(@RequestParam String batch, @RequestBody List<RejectionItem> items) {
        return importer.rejections(batch, items);
    }

    @PostMapping("/expenses")
    public SectionResult expenses(@RequestParam String batch, @RequestBody List<ExpenseItem> items) {
        return importer.expenses(batch, items);
    }
}
