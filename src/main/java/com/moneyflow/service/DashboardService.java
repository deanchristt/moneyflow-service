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

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Get all accounts
        List<Account> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);

        // Calculate total balance
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get transactions for the period
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenAndIsActiveTrue(userId, startDate, endDate);

        // Calculate totals
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netFlow = totalIncome.subtract(totalExpense);

        // Account summaries
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

        // Top expense categories
        List<DashboardSummary.CategorySummary> topExpenseCategories = getCategorySummaries(
                transactions, TransactionType.EXPENSE, totalExpense, 5);

        // Top income categories
        List<DashboardSummary.CategorySummary> topIncomeCategories = getCategorySummaries(
                transactions, TransactionType.INCOME, totalIncome, 5);

        return DashboardSummary.builder()
                .totalBalance(totalBalance)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netFlow(netFlow)
                .totalTransactions(transactions.size())
                .accountSummaries(accountSummaries)
                .topExpenseCategories(topExpenseCategories)
                .topIncomeCategories(topIncomeCategories)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyReport getMonthlyReport(Integer month, Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenAndIsActiveTrue(userId, startDate, endDate);

        // Calculate totals
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Daily flows
        Map<Integer, BigDecimal> dailyIncome = new HashMap<>();
        Map<Integer, BigDecimal> dailyExpense = new HashMap<>();

        for (Transaction transaction : transactions) {
            int day = transaction.getTransactionDate().getDayOfMonth();
            if (transaction.getType() == TransactionType.INCOME) {
                dailyIncome.merge(day, transaction.getAmount(), BigDecimal::add);
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                dailyExpense.merge(day, transaction.getAmount(), BigDecimal::add);
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

        // Category breakdowns
        List<MonthlyReport.CategoryBreakdown> expenseBreakdown = getCategoryBreakdown(
                transactions, TransactionType.EXPENSE, totalExpense);

        List<MonthlyReport.CategoryBreakdown> incomeBreakdown = getCategoryBreakdown(
                transactions, TransactionType.INCOME, totalIncome);

        return MonthlyReport.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netFlow(totalIncome.subtract(totalExpense))
                .dailyFlows(dailyFlows)
                .expenseBreakdown(expenseBreakdown)
                .incomeBreakdown(incomeBreakdown)
                .build();
    }

    private List<DashboardSummary.CategorySummary> getCategorySummaries(
            List<Transaction> transactions, TransactionType type, BigDecimal total, int limit) {

        Map<Long, List<Transaction>> byCategory = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(t -> t.getCategory().getId()));

        return byCategory.entrySet().stream()
                .map(entry -> {
                    List<Transaction> categoryTransactions = entry.getValue();
                    Transaction first = categoryTransactions.get(0);
                    BigDecimal amount = categoryTransactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return DashboardSummary.CategorySummary.builder()
                            .id(first.getCategory().getId())
                            .name(first.getCategory().getName())
                            .icon(first.getCategory().getIcon())
                            .color(first.getCategory().getColor())
                            .amount(amount)
                            .percentage(percentage)
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

        return byCategory.entrySet().stream()
                .map(entry -> {
                    List<Transaction> categoryTransactions = entry.getValue();
                    Transaction first = categoryTransactions.get(0);
                    BigDecimal amount = categoryTransactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return MonthlyReport.CategoryBreakdown.builder()
                            .categoryId(first.getCategory().getId())
                            .categoryName(first.getCategory().getName())
                            .icon(first.getCategory().getIcon())
                            .color(first.getCategory().getColor())
                            .amount(amount)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }
}
