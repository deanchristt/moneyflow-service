package com.moneyflow.controller;

import com.moneyflow.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/v1/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Data export endpoints")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/transactions/csv")
    @Operation(summary = "Export transactions to CSV")
    public ResponseEntity<byte[]> exportTransactionsToCSV(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {

        byte[] csvData = exportService.exportTransactionsToCSV(startDate, endDate, accountId);

        String filename = String.format("transactions_%s_to_%s.csv",
                startDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                endDate.format(DateTimeFormatter.BASIC_ISO_DATE));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/transactions/pdf")
    @Operation(summary = "Export transactions to PDF")
    public ResponseEntity<byte[]> exportTransactionsToPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {

        byte[] pdfData = exportService.exportTransactionsToPDF(startDate, endDate, accountId);

        String filename = String.format("transactions_%s_to_%s.pdf",
                startDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                endDate.format(DateTimeFormatter.BASIC_ISO_DATE));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/monthly-report/pdf")
    @Operation(summary = "Export monthly report to PDF")
    public ResponseEntity<byte[]> exportMonthlyReportToPDF(
            @RequestParam Integer month,
            @RequestParam Integer year) {

        byte[] pdfData = exportService.exportMonthlyReportToPDF(month, year);

        String filename = String.format("monthly_report_%d_%02d.pdf", year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
