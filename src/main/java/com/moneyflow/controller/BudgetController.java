package com.moneyflow.controller;

import com.moneyflow.model.dto.ApiResponse;
import com.moneyflow.model.dto.budget.BudgetResponse;
import com.moneyflow.model.dto.budget.CreateBudgetRequest;
import com.moneyflow.model.dto.budget.UpdateBudgetRequest;
import com.moneyflow.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management endpoints")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a new budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody CreateBudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get budgets by month and year")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByMonthAndYear(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        List<BudgetResponse> budgets = budgetService.getBudgetsByMonthAndYear(month, year);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get all budgets for a year")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByYear(
            @PathVariable Integer year) {
        List<BudgetResponse> budgets = budgetService.getBudgetsByYear(year);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(@PathVariable Long id) {
        BudgetResponse budget = budgetService.getBudgetById(id);
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        BudgetResponse response = budgetService.updateBudget(id, request);
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully", null));
    }
}
