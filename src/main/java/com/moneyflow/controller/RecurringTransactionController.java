package com.moneyflow.controller;

import com.moneyflow.model.dto.ApiResponse;
import com.moneyflow.model.dto.recurring.CreateRecurringTransactionRequest;
import com.moneyflow.model.dto.recurring.RecurringTransactionResponse;
import com.moneyflow.model.dto.recurring.UpdateRecurringTransactionRequest;
import com.moneyflow.service.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/recurring-transactions")
@RequiredArgsConstructor
@Tag(name = "Recurring Transactions", description = "Recurring transaction management endpoints")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    @Operation(summary = "Create a new recurring transaction")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> createRecurringTransaction(
            @Valid @RequestBody CreateRecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.createRecurringTransaction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recurring transaction created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all recurring transactions")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getAllRecurringTransactions() {
        List<RecurringTransactionResponse> transactions = recurringTransactionService.getAllRecurringTransactions();
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active (not paused) recurring transactions")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getActiveRecurringTransactions() {
        List<RecurringTransactionResponse> transactions = recurringTransactionService.getActiveRecurringTransactions();
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring transaction by ID")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getRecurringTransactionById(
            @PathVariable Long id) {
        RecurringTransactionResponse transaction = recurringTransactionService.getRecurringTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a recurring transaction")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> updateRecurringTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.updateRecurringTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recurring transaction")
    public ResponseEntity<ApiResponse<Void>> deleteRecurringTransaction(@PathVariable Long id) {
        recurringTransactionService.deleteRecurringTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction deleted successfully", null));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a recurring transaction")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> pauseRecurringTransaction(
            @PathVariable Long id) {
        RecurringTransactionResponse response = recurringTransactionService.pauseRecurringTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction paused", response));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a recurring transaction")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> resumeRecurringTransaction(
            @PathVariable Long id) {
        RecurringTransactionResponse response = recurringTransactionService.resumeRecurringTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction resumed", response));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute a recurring transaction manually")
    public ResponseEntity<ApiResponse<Void>> executeRecurringTransaction(@PathVariable Long id) {
        recurringTransactionService.executeRecurringTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction executed successfully", null));
    }
}
