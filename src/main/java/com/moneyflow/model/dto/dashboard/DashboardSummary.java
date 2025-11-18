package com.moneyflow.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netFlow;
    private Integer totalTransactions;
    private List<AccountSummary> accountSummaries;
    private List<CategorySummary> topExpenseCategories;
    private List<CategorySummary> topIncomeCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountSummary {
        private Long id;
        private String name;
        private String type;
        private BigDecimal balance;
        private String currency;
        private String icon;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
        private String icon;
        private String color;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer transactionCount;
    }
}
