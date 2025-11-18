package com.moneyflow.model.dto.recurring;

import com.moneyflow.model.enums.Frequency;
import com.moneyflow.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {

    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private Frequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecutionDate;
    private LocalDateTime lastExecutedAt;
    private Boolean isPaused;

    // Account info
    private Long accountId;
    private String accountName;

    // Category info
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    private Integer totalExecutions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
