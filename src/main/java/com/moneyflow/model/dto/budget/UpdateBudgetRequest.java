package com.moneyflow.model.dto.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetRequest {

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @DecimalMin(value = "0", message = "Alert threshold must be between 0 and 100")
    @DecimalMax(value = "100", message = "Alert threshold must be between 0 and 100")
    private BigDecimal alertThreshold;
}
