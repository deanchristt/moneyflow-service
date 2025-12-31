package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.model.dto.transaction.CreateTransactionRequest;
import com.moneyflow.model.dto.transaction.TransactionResponse;
import com.moneyflow.model.dto.transaction.UpdateTransactionRequest;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.AccountType;
import com.moneyflow.model.enums.CategoryType;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.CategoryRepository;
import com.moneyflow.repository.TagRepository;
import com.moneyflow.repository.TransactionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private BudgetAlertService budgetAlertService;
    @Mock private TeamPermissionService teamPermissionService;

    @InjectMocks private TransactionService service;

    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(teamPermissionService.canAccessAccount(eq(1L), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    private User user() {
        User u = User.builder().email("u@ex.com").build();
        u.setId(1L);
        return u;
    }

    private Account account(long id, AccountType type, String balance) {
        Account a = Account.builder().name("acc" + id).type(type).user(user())
                .balance(new BigDecimal(balance)).build();
        a.setId(id);
        return a;
    }

    private Category category() {
        Category c = Category.builder().name("Food").type(CategoryType.EXPENSE).build();
        c.setId(5L);
        return c;
    }

    @Test
    void updateMovingAccountReversesOldAndAppliesNew() {
        Account accA = account(1, AccountType.BANK, "400"); // already reflects the -100 expense
        Account accB = account(2, AccountType.BANK, "1000");
        Transaction tx = Transaction.builder()
                .user(user()).account(accA).category(category())
                .type(TransactionType.EXPENSE).amount(new BigDecimal("100"))
                .description("groceries").transactionDate(LocalDate.now()).build();
        tx.setId(10L);

        when(transactionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(tx));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(accB));

        TransactionResponse res = service.updateTransaction(10L,
                UpdateTransactionRequest.builder().accountId(2L).build());

        assertThat(accA.getBalance()).isEqualByComparingTo("500"); // reversal added back
        assertThat(accB.getBalance()).isEqualByComparingTo("900"); // new deduction
        assertThat(res.getAccountId()).isEqualTo(2L);
    }

    @Test
    void createExpenseBlockedWhenOverdraftGuardEnabled() {
        ReflectionTestUtils.setField(service, "enforceSufficientBalance", true);
        Account cash = account(1, AccountType.CASH, "0");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(cash));
        when(categoryRepository.findByIdAndAvailableForUser(5L, 1L)).thenReturn(Optional.of(category()));

        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId(1L).categoryId(5L).type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50")).build();

        assertThatThrownBy(() -> service.createTransaction(req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createExpenseAllowedWhenGuardDisabled() {
        Account cash = account(1, AccountType.CASH, "0");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(cash));
        when(categoryRepository.findByIdAndAvailableForUser(5L, 1L)).thenReturn(Optional.of(category()));

        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId(1L).categoryId(5L).type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50")).build();

        TransactionResponse res = service.createTransaction(req);

        assertThat(res.getAmount()).isEqualByComparingTo("50");
        assertThat(cash.getBalance()).isEqualByComparingTo("-50"); // allowed to go negative by default
    }
}
