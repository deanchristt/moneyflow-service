package com.moneyflow.model.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private BigDecimal amount;
    private Integer month;
    private Integer year;
    private BigDecimal alertThreshold;
    private BigDecimal spent;
    private BigDecimal remaining;
    private BigDecimal percentageUsed;
    private Boolean isOverBudget;
    private Boolean isAlertTriggered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
