package com.moneyflow.model.dto.savings;

import com.moneyflow.model.enums.GoalStatus;
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
public class UpdateSavingsGoalRequest {

    private String name;

    @Positive(message = "Target amount must be positive")
    private BigDecimal targetAmount;

    private LocalDate targetDate;

    private String icon;

    private String color;

    private GoalStatus status;
}
