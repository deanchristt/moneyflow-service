package com.moneyflow.controller;

import com.moneyflow.model.dto.ApiResponse;
import com.moneyflow.model.dto.dashboard.DashboardSummary;
import com.moneyflow.model.dto.dashboard.MonthlyReport;
import com.moneyflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard and reports endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary for date range")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        DashboardSummary summary = dashboardService.getDashboardSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/monthly-report")
    @Operation(summary = "Get monthly report")
    public ResponseEntity<ApiResponse<MonthlyReport>> getMonthlyReport(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        MonthlyReport report = dashboardService.getMonthlyReport(month, year);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
