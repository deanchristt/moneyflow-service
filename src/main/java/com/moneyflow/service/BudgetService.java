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
import java.util.List;
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

        Category category = categoryRepository.findById(request.getCategoryId())
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
        return mapToResponse(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonthAndYear(Integer month, Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        return budgetRepository.findByUserIdAndMonthAndYearAndIsActiveTrue(userId, month, year)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByYear(Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        return budgetRepository.findByUserIdAndYearAndIsActiveTrue(userId, year)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));
        return mapToResponse(budget);
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
        return mapToResponse(budget);
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

    private BudgetResponse mapToResponse(Budget budget) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Calculate date range for the budget period
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // Get spent amount for this category in this period
        BigDecimal spent = transactionRepository.sumAmountByCategoryAndDateRange(
                userId, budget.getCategory().getId(), startDate, endDate);
        spent = spent != null ? spent : BigDecimal.ZERO;

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
