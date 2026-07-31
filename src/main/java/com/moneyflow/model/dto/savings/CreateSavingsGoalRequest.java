package com.moneyflow.model.dto.savings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSavingsGoalRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be positive")
    private BigDecimal targetAmount;

    @Positive(message = "Initial amount must be positive")
    private BigDecimal initialAmount;

    private LocalDate targetDate;

    private Long accountId;

    private String icon;

    private String color;
}
