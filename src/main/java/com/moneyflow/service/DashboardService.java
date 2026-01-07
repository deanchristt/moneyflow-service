package com.moneyflow.service;

import com.moneyflow.model.dto.dashboard.DashboardSummary;
import com.moneyflow.model.dto.dashboard.MonthlyReport;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TeamPermissionService teamPermissionService;
    private final CurrencyService currencyService;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Own accounts plus accounts shared with the user's team.
        List<Account> accounts = accountRepository.findAllAccessibleByUser(userId);

        BigDecimal totalBalance = accounts.stream()
                .map(a -> currencyService.toBase(a.getBalance(), a.getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> transactions = transactionsFor(accounts, startDate, endDate);

        BigDecimal totalIncome = sumBase(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumBase(transactions, TransactionType.EXPENSE);
        BigDecimal netFlow = totalIncome.subtract(totalExpense);

        List<DashboardSummary.AccountSummary> accountSummaries = accounts.stream()
                .map(account -> DashboardSummary.AccountSummary.builder()
                        .id(account.getId())
                        .name(account.getName())
                        .type(account.getType().name())
                        .balance(account.getBalance())
                        .currency(account.getCurrency())
                        .icon(account.getIcon())
                        .color(account.getColor())
                        .build())
                .collect(Collectors.toList());

        return DashboardSummary.builder()
                .baseCurrency(currencyService.getBaseCurrency())
                .totalBalance(totalBalance)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netFlow(netFlow)
                .totalTransactions(transactions.size())
                .accountSummaries(accountSummaries)
                .topExpenseCategories(getCategorySummaries(transactions, TransactionType.EXPENSE, totalExpense, 5))
                .topIncomeCategories(getCategorySummaries(transactions, TransactionType.INCOME, totalIncome, 5))
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyReport getMonthlyReport(Integer month, Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Account> accounts = accountRepository.findAllAccessibleByUser(userId);
        List<Transaction> transactions = transactionsFor(accounts, startDate, endDate);

        BigDecimal totalIncome = sumBase(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumBase(transactions, TransactionType.EXPENSE);

        Map<Integer, BigDecimal> dailyIncome = new HashMap<>();
        Map<Integer, BigDecimal> dailyExpense = new HashMap<>();
        for (Transaction transaction : transactions) {
            int day = transaction.getTransactionDate().getDayOfMonth();
            BigDecimal base = baseAmount(transaction);
            if (transaction.getType() == TransactionType.INCOME) {
                dailyIncome.merge(day, base, BigDecimal::add);
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                dailyExpense.merge(day, base, BigDecimal::add);
            }
        }

        List<MonthlyReport.DailyFlow> dailyFlows = new ArrayList<>();
        int daysInMonth = endDate.getDayOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            BigDecimal income = dailyIncome.getOrDefault(day, BigDecimal.ZERO);
            BigDecimal expense = dailyExpense.getOrDefault(day, BigDecimal.ZERO);
            dailyFlows.add(MonthlyReport.DailyFlow.builder()
                    .day(day)
                    .income(income)
                    .expense(expense)
                    .net(income.subtract(expense))
                    .build());
        }

        return MonthlyReport.builder()
                .month(month)
                .year(year)
                .baseCurrency(currencyService.getBaseCurrency())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netFlow(totalIncome.subtract(totalExpense))
                .dailyFlows(dailyFlows)
                .expenseBreakdown(getCategoryBreakdown(transactions, TransactionType.EXPENSE, totalExpense))
                .incomeBreakdown(getCategoryBreakdown(transactions, TransactionType.INCOME, totalIncome))
                .build();
    }

    private List<Transaction> transactionsFor(List<Account> accounts, LocalDate startDate, LocalDate endDate) {
        List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdInAndTransactionDateBetweenAndIsActiveTrue(
                accountIds, startDate, endDate);
    }

    /** Transaction amount converted to the base currency using its account's currency. */
    private BigDecimal baseAmount(Transaction t) {
        return currencyService.toBase(t.getAmount(), t.getAccount().getCurrency());
    }

    private BigDecimal sumBase(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(this::baseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DashboardSummary.CategorySummary> getCategorySummaries(
            List<Transaction> transactions, TransactionType type, BigDecimal total, int limit) {

        Map<Long, List<Transaction>> byCategory = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(t -> t.getCategory().getId()));

        return byCategory.values().stream()
                .map(categoryTransactions -> {
                    Transaction first = categoryTransactions.get(0);
                    BigDecimal amount = categoryTransactions.stream()
                            .map(this::baseAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return DashboardSummary.CategorySummary.builder()
                            .id(first.getCategory().getId())
                            .name(first.getCategory().getName())
                            .icon(first.getCategory().getIcon())
                            .color(first.getCategory().getColor())
                            .amount(amount)
                            .percentage(percentage(amount, total))
                            .transactionCount(categoryTransactions.size())
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<MonthlyReport.CategoryBreakdown> getCategoryBreakdown(
            List<Transaction> transactions, TransactionType type, BigDecimal total) {

        Map<Long, List<Transaction>> byCategory = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(t -> t.getCategory().getId()));

        return byCategory.values().stream()
                .map(categoryTransactions -> {
                    Transaction first = categoryTransactions.get(0);
                    BigDecimal amount = categoryTransactions.stream()
                            .map(this::baseAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return MonthlyReport.CategoryBreakdown.builder()
                            .categoryId(first.getCategory().getId())
                            .categoryName(first.getCategory().getName())
                            .icon(first.getCategory().getIcon())
                            .color(first.getCategory().getColor())
                            .amount(amount)
                            .percentage(percentage(amount, total))
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        return total.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}
