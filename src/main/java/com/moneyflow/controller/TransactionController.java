package com.moneyflow.controller;

import com.moneyflow.model.dto.ApiResponse;
import com.moneyflow.model.dto.transaction.CreateTransactionRequest;
import com.moneyflow.model.dto.transaction.TransactionFilterRequest;
import com.moneyflow.model.dto.transaction.TransactionResponse;
import com.moneyflow.model.dto.transaction.UpdateTransactionRequest;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get transactions with filters")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(type)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<TransactionResponse> transactions = transactionService.getTransactions(filter);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable Long id) {
        TransactionResponse transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }

    @GetMapping("/summary/income")
    @Operation(summary = "Get total income for date range")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalIncome(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal totalIncome = transactionService.getTotalIncome(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(totalIncome));
    }

    @GetMapping("/summary/expense")
    @Operation(summary = "Get total expense for date range")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal totalExpense = transactionService.getTotalExpense(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(totalExpense));
    }
}
