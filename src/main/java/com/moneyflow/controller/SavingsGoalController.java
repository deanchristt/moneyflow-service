package com.moneyflow.controller;

import com.moneyflow.model.dto.ApiResponse;
import com.moneyflow.model.dto.savings.ContributionRequest;
import com.moneyflow.model.dto.savings.CreateSavingsGoalRequest;
import com.moneyflow.model.dto.savings.SavingsGoalResponse;
import com.moneyflow.model.dto.savings.UpdateSavingsGoalRequest;
import com.moneyflow.service.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/savings-goals")
@RequiredArgsConstructor
@Tag(name = "Savings Goals", description = "Savings goal management endpoints")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @PostMapping
    @Operation(summary = "Create a savings goal")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> createGoal(
            @Valid @RequestBody CreateSavingsGoalRequest request) {
        SavingsGoalResponse response = savingsGoalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Savings goal created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all savings goals")
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getGoals() {
        return ResponseEntity.ok(ApiResponse.success(savingsGoalService.getGoals()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a savings goal by ID")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> getGoalById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(savingsGoalService.getGoalById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a savings goal")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSavingsGoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Savings goal updated successfully",
                savingsGoalService.updateGoal(id, request)));
    }

    @PostMapping("/{id}/contribute")
    @Operation(summary = "Add funds to a savings goal")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> contribute(
            @PathVariable Long id,
            @Valid @RequestBody ContributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Contribution added",
                savingsGoalService.contribute(id, request)));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw funds from a savings goal")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody ContributionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal applied",
                savingsGoalService.withdraw(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a savings goal")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable Long id) {
        savingsGoalService.deleteGoal(id);
        return ResponseEntity.ok(ApiResponse.success("Savings goal deleted successfully", null));
    }
}
