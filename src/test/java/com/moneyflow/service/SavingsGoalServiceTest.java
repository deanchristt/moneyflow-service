package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.model.dto.savings.ContributionRequest;
import com.moneyflow.model.dto.savings.CreateSavingsGoalRequest;
import com.moneyflow.model.dto.savings.SavingsGoalResponse;
import com.moneyflow.model.entity.SavingsGoal;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.GoalStatus;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.SavingsGoalRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavingsGoalServiceTest {

    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SavingsGoalService service;

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId()));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    private User userWithId() {
        User u = User.builder().email("u@ex.com").build();
        u.setId(1L);
        return u;
    }

    private SavingsGoal goal(BigDecimal current, BigDecimal target, GoalStatus status) {
        SavingsGoal g = SavingsGoal.builder()
                .user(userWithId()).name("Vacation")
                .targetAmount(target).currentAmount(current).status(status)
                .build();
        g.setId(5L);
        return g;
    }

    @Test
    void createComputesProgress() {
        CreateSavingsGoalRequest req = CreateSavingsGoalRequest.builder()
                .name("Vacation").targetAmount(new BigDecimal("1000"))
                .initialAmount(new BigDecimal("200")).build();

        SavingsGoalResponse res = service.createGoal(req);

        assertThat(res.getCurrentAmount()).isEqualByComparingTo("200");
        assertThat(res.getRemaining()).isEqualByComparingTo("800");
        assertThat(res.getPercentageComplete()).isEqualByComparingTo("20.00");
        assertThat(res.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void contributeReachingTargetCompletesGoal() {
        when(savingsGoalRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(goal(new BigDecimal("200"), new BigDecimal("1000"), GoalStatus.ACTIVE)));

        SavingsGoalResponse res = service.contribute(5L,
                ContributionRequest.builder().amount(new BigDecimal("800")).build());

        assertThat(res.getCurrentAmount()).isEqualByComparingTo("1000");
        assertThat(res.getIsCompleted()).isTrue();
        assertThat(res.getStatus()).isEqualTo(GoalStatus.COMPLETED);
    }

    @Test
    void withdrawMoreThanSavedIsRejected() {
        when(savingsGoalRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(goal(new BigDecimal("100"), new BigDecimal("1000"), GoalStatus.ACTIVE)));

        assertThatThrownBy(() -> service.withdraw(5L,
                ContributionRequest.builder().amount(new BigDecimal("200")).build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void withdrawReopensCompletedGoalBelowTarget() {
        when(savingsGoalRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(goal(new BigDecimal("1000"), new BigDecimal("1000"), GoalStatus.COMPLETED)));

        SavingsGoalResponse res = service.withdraw(5L,
                ContributionRequest.builder().amount(new BigDecimal("300")).build());

        assertThat(res.getCurrentAmount()).isEqualByComparingTo("700");
        assertThat(res.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }
}
