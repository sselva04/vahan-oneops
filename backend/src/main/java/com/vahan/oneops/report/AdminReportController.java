package com.vahan.oneops.report;

import com.vahan.oneops.report.ReportDtos.ExpenseInput;
import com.vahan.oneops.report.ReportDtos.MonthDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Admin-only (SecurityConfig protects everything under /api/admin). */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reports;

    public AdminReportController(ReportService reports) {
        this.reports = reports;
    }

    /** from = "2026-04", count = how many months (default 12). */
    @GetMapping("/months")
    public List<MonthDto> months(@RequestParam String from, @RequestParam(defaultValue = "12") int count) {
        return reports.months(ReportService.parse(from), count);
    }

    @GetMapping("/monthly/{month}")
    public MonthDto month(@PathVariable String month) {
        return reports.month(ReportService.parse(month));
    }

    @PutMapping("/monthly/{month}/expenses")
    public MonthDto saveExpenses(@PathVariable String month, @Valid @RequestBody ExpenseInput request) {
        return reports.saveExpenses(ReportService.parse(month), request);
    }

    @GetMapping("/monthly/{month}/csv")
    public ResponseEntity<byte[]> csv(@PathVariable String month) {
        var ym = ReportService.parse(month);
        // a leading byte-order mark makes Excel read the file as UTF-8
        byte[] body = ("﻿" + reports.csv(ym)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vahan-" + ym + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
