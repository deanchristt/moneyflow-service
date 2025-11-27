package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.budget.BudgetResponse;
import com.moneyflow.model.dto.budget.CreateBudgetRequest;
import com.moneyflow.model.dto.budget.UpdateBudgetRequest;
import com.moneyflow.model.entity.Budget;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.BudgetRepository;
import com.moneyflow.repository.CategoryRepository;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Check if budget already exists for this category/month/year
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                userId, request.getCategoryId(), request.getMonth(), request.getYear())) {
            throw new BadRequestException("Budget already exists for this category and period");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Category category = categoryRepository.findByIdAndAvailableForUser(request.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .alertThreshold(request.getAlertThreshold() != null
                        ? request.getAlertThreshold()
                        : new BigDecimal("80.00"))
                .build();

        budget = budgetRepository.save(budget);
        return mapToResponse(budget, computeSpent(budget, userId));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonthAndYear(Integer month, Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndIsActiveTrue(userId, month, year);
        return mapWithBatchedSpent(userId, budgets);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByYear(Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndIsActiveTrue(userId, year);
        return mapWithBatchedSpent(userId, budgets);
    }

    /**
     * Budgets for the current month that have crossed their alert threshold or gone over budget.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getTriggeredBudgets() {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndIsActiveTrue(
                userId, now.getMonthValue(), now.getYear());
        return mapWithBatchedSpent(userId, budgets).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsAlertTriggered()) || Boolean.TRUE.equals(b.getIsOverBudget()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));
        return mapToResponse(budget, computeSpent(budget, userId));
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, UpdateBudgetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));

        if (request.getAmount() != null) {
            budget.setAmount(request.getAmount());
        }

        if (request.getAlertThreshold() != null) {
            budget.setAlertThreshold(request.getAlertThreshold());
        }

        budget = budgetRepository.save(budget);
        return mapToResponse(budget, computeSpent(budget, userId));
    }

    @Transactional
    public void deleteBudget(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));

        // Soft delete
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }

    /**
     * Spent amount for a single budget, counting only the transaction type that
     * matches the category (e.g. EXPENSE for an expense budget).
     */
    private BigDecimal computeSpent(Budget budget, Long userId) {
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        TransactionType type = TransactionType.valueOf(budget.getCategory().getType().name());
        BigDecimal spent = transactionRepository.sumAmountByCategoryTypeAndDateRange(
                userId, budget.getCategory().getId(), type, startDate, endDate);
        return spent != null ? spent : BigDecimal.ZERO;
    }

    /**
     * Maps a list of budgets, batching the "spent" aggregation to one query per
     * distinct period instead of one query per budget (avoids N+1).
     */
    private List<BudgetResponse> mapWithBatchedSpent(Long userId, List<Budget> budgets) {
        Map<YearMonth, List<Budget>> byPeriod = budgets.stream()
                .collect(Collectors.groupingBy(b -> YearMonth.of(b.getYear(), b.getMonth())));

        List<BudgetResponse> result = new ArrayList<>();
        for (Map.Entry<YearMonth, List<Budget>> entry : byPeriod.entrySet()) {
            YearMonth ym = entry.getKey();
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            List<Budget> periodBudgets = entry.getValue();

            List<Long> categoryIds = periodBudgets.stream()
                    .map(b -> b.getCategory().getId())
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, BigDecimal> sums = new HashMap<>();
            for (Object[] row : transactionRepository.sumByCategoriesAndPeriodGrouped(userId, categoryIds, start, end)) {
                Long categoryId = (Long) row[0];
                TransactionType type = (TransactionType) row[1];
                BigDecimal sum = (BigDecimal) row[2];
                sums.put(spentKey(categoryId, type), sum);
            }

            for (Budget budget : periodBudgets) {
                TransactionType type = TransactionType.valueOf(budget.getCategory().getType().name());
                BigDecimal spent = sums.getOrDefault(
                        spentKey(budget.getCategory().getId(), type), BigDecimal.ZERO);
                result.add(mapToResponse(budget, spent));
            }
        }
        return result;
    }

    private String spentKey(Long categoryId, TransactionType type) {
        return categoryId + ":" + type.name();
    }

    private BudgetResponse mapToResponse(Budget budget, BigDecimal spent) {
        BigDecimal remaining = budget.getAmount().subtract(spent);
        BigDecimal percentageUsed = BigDecimal.ZERO;

        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = spent.multiply(new BigDecimal("100"))
                    .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
        }

        boolean isOverBudget = spent.compareTo(budget.getAmount()) > 0;
        boolean isAlertTriggered = percentageUsed.compareTo(budget.getAlertThreshold()) >= 0;

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .categoryIcon(budget.getCategory().getIcon())
                .categoryColor(budget.getCategory().getColor())
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .alertThreshold(budget.getAlertThreshold())
                .spent(spent)
                .remaining(remaining)
                .percentageUsed(percentageUsed)
                .isOverBudget(isOverBudget)
                .isAlertTriggered(isAlertTriggered)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}
