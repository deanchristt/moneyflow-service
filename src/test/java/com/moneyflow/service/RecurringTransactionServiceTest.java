package com.moneyflow.service;

import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.RecurringTransaction;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.CategoryType;
import com.moneyflow.model.enums.Frequency;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.CategoryRepository;
import com.moneyflow.repository.RecurringTransactionRepository;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository recurringTransactionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private BudgetAlertService budgetAlertService;

    @InjectMocks private RecurringTransactionService service;

    private RecurringTransaction dailyRecurring(LocalDate nextExecution, LocalDate endDate) {
        User user = User.builder().email("u@example.com").build();
        user.setId(1L);
        Category category = Category.builder().name("Food").type(CategoryType.EXPENSE).build();
        category.setId(10L);
        Account account = Account.builder().name("Cash").balance(BigDecimal.ZERO).build();
        account.setId(20L);

        return RecurringTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10.00"))
                .description("Coffee")
                .frequency(Frequency.DAILY)
                .startDate(nextExecution)
                .nextExecutionDate(nextExecution)
                .endDate(endDate)
                .isPaused(false)
                .build();
    }

    @Test
    void backfillsEveryMissedPeriodUpToToday() {
        LocalDate today = LocalDate.now();
        RecurringTransaction recurring = dailyRecurring(today.minusDays(3), null);
        when(recurringTransactionRepository.findDueRecurringTransactions(any()))
                .thenReturn(List.of(recurring));

        int processed = service.processDueRecurringTransactions();

        // today-3, today-2, today-1, today => 4 occurrences
        assertThat(processed).isEqualTo(4);
        assertThat(recurring.getNextExecutionDate()).isEqualTo(today.plusDays(1));
    }

    @Test
    void stopsBackfillAtEndDate() {
        LocalDate today = LocalDate.now();
        RecurringTransaction recurring = dailyRecurring(today.minusDays(3), today.minusDays(1));
        when(recurringTransactionRepository.findDueRecurringTransactions(any()))
                .thenReturn(List.of(recurring));

        int processed = service.processDueRecurringTransactions();

        // today-3, today-2, today-1 (endDate) => 3 occurrences, then stops
        assertThat(processed).isEqualTo(3);
        assertThat(recurring.getNextExecutionDate()).isEqualTo(today);
    }
}
