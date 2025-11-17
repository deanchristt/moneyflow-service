package com.moneyflow.model.dto.transaction;

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
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private String note;
    private LocalDate transactionDate;
    private String referenceNumber;

    // Account info
    private Long accountId;
    private String accountName;

    // Category info
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;

    // Transfer info
    private Long transferToAccountId;
    private String transferToAccountName;

    // Recurring info
    private Long recurringTransactionId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
