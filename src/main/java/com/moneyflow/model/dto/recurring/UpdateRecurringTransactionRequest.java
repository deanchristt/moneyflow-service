package com.moneyflow.model.dto.recurring;

import com.moneyflow.model.enums.Frequency;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class UpdateRecurringTransactionRequest {

    private Long categoryId;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Frequency frequency;

    private LocalDate endDate;

    private Boolean isPaused;
}
