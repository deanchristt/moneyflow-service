package com.moneyflow.service;

import com.moneyflow.model.entity.Budget;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.notification.NotificationService;
import com.moneyflow.repository.BudgetRepository;
import com.moneyflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Evaluates the budget for a category/period after spending changes and sends a
 * one-shot alert when the alert threshold or the budget itself is crossed. The
 * {@code alertSentAt} marker prevents repeat notifications; it is cleared when
 * spending drops back below the threshold so a later breach can alert again.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetAlertService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Transactional
    public void evaluateForCategory(Long userId, Long categoryId, LocalDate transactionDate) {
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                        userId, categoryId, transactionDate.getMonthValue(), transactionDate.getYear())
                .filter(budget -> Boolean.TRUE.equals(budget.getIsActive()))
                .ifPresent(budget -> evaluate(userId, budget));
    }

    private void evaluate(Long userId, Budget budget) {
        BigDecimal spent = computeSpent(budget, userId);

        BigDecimal percentageUsed = BigDecimal.ZERO;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = spent.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
        }

        boolean overBudget = spent.compareTo(budget.getAmount()) > 0;
        boolean alertTriggered = percentageUsed.compareTo(budget.getAlertThreshold()) >= 0;
        boolean triggered = overBudget || alertTriggered;

        if (triggered && budget.getAlertSentAt() == null) {
            notificationService.sendBudgetAlert(budget.getUser(), budget, spent, percentageUsed, overBudget);
            budget.setAlertSentAt(LocalDateTime.now());
            budgetRepository.save(budget);
        } else if (!triggered && budget.getAlertSentAt() != null) {
            // Spending fell back below the threshold; re-arm the alert.
            budget.setAlertSentAt(null);
            budgetRepository.save(budget);
        }
    }

    private BigDecimal computeSpent(Budget budget, Long userId) {
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        TransactionType type = TransactionType.valueOf(budget.getCategory().getType().name());
        BigDecimal spent = transactionRepository.sumAmountByCategoryTypeAndDateRange(
                userId, budget.getCategory().getId(), type, startDate, endDate);
        return spent != null ? spent : BigDecimal.ZERO;
    }
}
