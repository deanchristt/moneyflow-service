package com.moneyflow.model.dto.transaction;

import com.moneyflow.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterRequest {

    private Long accountId;
    private Long categoryId;
    private TransactionType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
