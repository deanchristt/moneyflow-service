package com.moneyflow.service;

import com.moneyflow.model.entity.Budget;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.CategoryType;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.notification.NotificationService;
import com.moneyflow.repository.BudgetRepository;
import com.moneyflow.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BudgetAlertServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private BudgetAlertService service;

    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

    private Budget budget(LocalDateTime alertSentAt) {
        User u = User.builder().email("u@ex.com").build();
        u.setId(1L);
        Category c = Category.builder().name("Food").type(CategoryType.EXPENSE).build();
        c.setId(5L);
        Budget b = Budget.builder()
                .user(u).category(c).amount(new BigDecimal("100"))
                .month(1).year(2026).alertThreshold(new BigDecimal("80.00"))
                .alertSentAt(alertSentAt).build();
        b.setId(7L);
        b.setIsActive(true);
        return b;
    }

    private void stubBudget(Budget b) {
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(1L, 5L, 1, 2026))
                .thenReturn(Optional.of(b));
    }

    private void stubSpent(String amount) {
        when(transactionRepository.sumAmountByCategoryTypeAndDateRange(
                eq(1L), eq(5L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal(amount));
    }

    @Test
    void firesAlertOnceWhenThresholdCrossed() {
        Budget b = budget(null);
        stubBudget(b);
        stubSpent("90"); // 90% >= 80% threshold

        service.evaluateForCategory(1L, 5L, DATE);

        verify(notificationService, times(1))
                .sendBudgetAlert(any(), any(), any(), any(), anyBoolean());
        assertThat(b.getAlertSentAt()).isNotNull();
    }

    @Test
    void doesNotResendWhenAlreadyAlerted() {
        Budget b = budget(LocalDateTime.of(2026, 1, 10, 9, 0));
        stubBudget(b);
        stubSpent("95");

        service.evaluateForCategory(1L, 5L, DATE);

        verify(notificationService, never())
                .sendBudgetAlert(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rearmsWhenSpendingDropsBelowThreshold() {
        Budget b = budget(LocalDateTime.of(2026, 1, 10, 9, 0));
        stubBudget(b);
        stubSpent("50"); // back under threshold

        service.evaluateForCategory(1L, 5L, DATE);

        verify(notificationService, never())
                .sendBudgetAlert(any(), any(), any(), any(), anyBoolean());
        assertThat(b.getAlertSentAt()).isNull();
    }
}
