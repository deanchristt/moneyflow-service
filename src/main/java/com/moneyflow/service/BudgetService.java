package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import com.moneyflow.model.dto.budget.BudgetResponse;
import com.moneyflow.model.dto.budget.CreateBudgetRequest;
import com.moneyflow.model.dto.budget.UpdateBudgetRequest;
import com.moneyflow.model.entity.Budget;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.Team;
import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.TeamRole;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    private final TeamPermissionService teamPermissionService;

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                userId, request.getCategoryId(), request.getMonth(), request.getYear())) {
            throw new BadRequestException("Budget already exists for this category and period");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Category category = categoryRepository.findByIdAndAvailableForUser(request.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Team team = null;
        if (Boolean.TRUE.equals(request.getTeamShared())) {
            TeamRole role = teamPermissionService.role(userId);
            if (role != TeamRole.OWNER && role != TeamRole.ADMIN) {
                throw new UnauthorizedException("Only a team owner or admin can create a team budget");
            }
            if (category.getTeam() == null) {
                throw new BadRequestException("Team budgets require a team-shared category");
            }
            team = teamPermissionService.membership(userId).orElseThrow().getTeam();
        }

        Budget budget = Budget.builder()
                .user(user)
                .team(team)
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
        return budgetRepository.findVisibleByMonthAndYear(userId, month, year).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByYear(Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        return budgetRepository.findVisibleByYear(userId, year).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Budgets for the current month that have crossed their alert threshold or gone over budget.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getTriggeredBudgets() {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate now = LocalDate.now();
        return budgetRepository.findVisibleByMonthAndYear(userId, now.getMonthValue(), now.getYear()).stream()
                .map(this::mapToResponse)
                .filter(b -> Boolean.TRUE.equals(b.getIsAlertTriggered()) || Boolean.TRUE.equals(b.getIsOverBudget()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Budget budget = budgetRepository.findById(id)
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> canView(userId, b))
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
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }

    private boolean canView(Long userId, Budget budget) {
        if (budget.getUser().getId().equals(userId)) {
            return true;
        }
        Long teamId = teamPermissionService.teamId(userId);
        return budget.getTeam() != null && teamId != null && budget.getTeam().getId().equals(teamId);
    }

    /**
     * Spent for a budget, converted to the base currency. Team budgets aggregate the
     * category's spending across all members; personal budgets only the owner's.
     */
    private BigDecimal computeSpent(Budget budget) {
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        TransactionType type = TransactionType.valueOf(budget.getCategory().getType().name());
        Long categoryId = budget.getCategory().getId();

        List<Transaction> transactions = budget.getTeam() != null
                ? transactionRepository.findSpentTransactionsForCategory(categoryId, type, startDate, endDate)
                : transactionRepository.findSpentTransactionsForUser(
                        budget.getUser().getId(), categoryId, type, startDate, endDate);

        return transactions.stream()
                .map(t -> currencyService.toBase(t.getAmount(), t.getAccount().getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BudgetResponse mapToResponse(Budget budget) {
        BigDecimal spent = computeSpent(budget);
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
                .teamShared(budget.getTeam() != null)
                .baseCurrency(currencyService.getBaseCurrency())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}
