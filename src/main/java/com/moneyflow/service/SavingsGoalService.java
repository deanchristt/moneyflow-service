package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.savings.ContributionRequest;
import com.moneyflow.model.dto.savings.CreateSavingsGoalRequest;
import com.moneyflow.model.dto.savings.SavingsGoalResponse;
import com.moneyflow.model.dto.savings.UpdateSavingsGoalRequest;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.SavingsGoal;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.GoalStatus;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.SavingsGoalRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public SavingsGoalResponse createGoal(CreateSavingsGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = null;
        if (request.getAccountId() != null) {
            account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));
        }

        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .account(account)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getInitialAmount() != null ? request.getInitialAmount() : BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .icon(request.getIcon())
                .color(request.getColor())
                .status(GoalStatus.ACTIVE)
                .build();

        applyCompletion(goal);
        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getGoals() {
        Long userId = SecurityUtils.getCurrentUserId();
        return savingsGoalRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getGoalById(Long id) {
        return mapToResponse(getOwnedGoal(id));
    }

    @Transactional
    public SavingsGoalResponse updateGoal(Long id, UpdateSavingsGoalRequest request) {
        SavingsGoal goal = getOwnedGoal(id);

        if (request.getName() != null) {
            goal.setName(request.getName());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getIcon() != null) {
            goal.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            goal.setColor(request.getColor());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }

        applyCompletion(goal);
        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse contribute(Long id, ContributionRequest request) {
        SavingsGoal goal = getOwnedGoal(id);
        goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));
        applyCompletion(goal);
        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse withdraw(Long id, ContributionRequest request) {
        SavingsGoal goal = getOwnedGoal(id);
        BigDecimal newAmount = goal.getCurrentAmount().subtract(request.getAmount());
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Cannot withdraw more than the current saved amount");
        }
        goal.setCurrentAmount(newAmount);
        // Re-open a completed goal if it drops back below target
        if (goal.getStatus() == GoalStatus.COMPLETED && newAmount.compareTo(goal.getTargetAmount()) < 0) {
            goal.setStatus(GoalStatus.ACTIVE);
        }
        goal = savingsGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public void deleteGoal(Long id) {
        SavingsGoal goal = getOwnedGoal(id);
        goal.setIsActive(false);
        savingsGoalRepository.save(goal);
    }

    private SavingsGoal getOwnedGoal(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return savingsGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingsGoal", "id", id));
    }

    private void applyCompletion(SavingsGoal goal) {
        if (goal.getStatus() == GoalStatus.ACTIVE
                && goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        }
    }

    private SavingsGoalResponse mapToResponse(SavingsGoal goal) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        BigDecimal percentage = BigDecimal.ZERO;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        }

        return SavingsGoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remaining(remaining)
                .percentageComplete(percentage)
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .isCompleted(goal.getStatus() == GoalStatus.COMPLETED)
                .accountId(goal.getAccount() != null ? goal.getAccount().getId() : null)
                .accountName(goal.getAccount() != null ? goal.getAccount().getName() : null)
                .icon(goal.getIcon())
                .color(goal.getColor())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
