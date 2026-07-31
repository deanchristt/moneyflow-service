package com.moneyflow.service;

import com.moneyflow.config.CurrencyProperties;
import com.moneyflow.model.dto.account.BalanceSummaryResponse;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.AccountType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamPermissionService teamPermissionService;

    private AccountService service;
    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        CurrencyProperties props = new CurrencyProperties();
        props.setBase("USD");
        props.setRates(Map.of("USD", BigDecimal.ONE, "IDR", new BigDecimal("0.000063")));
        CurrencyService currencyService = new CurrencyService(props);
        service = new AccountService(accountRepository, userRepository, teamPermissionService, currencyService);

        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    private Account account(long id, String currency, String balance) {
        User u = User.builder().email("u@ex.com").build();
        u.setId(1L);
        Account a = Account.builder().name("acc" + id).type(AccountType.BANK)
                .user(u).currency(currency).balance(new BigDecimal(balance)).build();
        a.setId(id);
        return a;
    }

    @Test
    void totalBalanceConvertsAcrossCurrencies() {
        when(accountRepository.findAllAccessibleByUser(1L)).thenReturn(List.of(
                account(1, "USD", "100"),
                account(2, "IDR", "1000000"))); // 1,000,000 IDR = 63 USD

        // 100 USD + 63 USD = 163 (not a raw 1,000,100 sum)
        assertThat(service.getTotalBalance()).isEqualByComparingTo("163");
    }

    @Test
    void balanceSummaryBreaksDownByCurrency() {
        when(accountRepository.findAllAccessibleByUser(1L)).thenReturn(List.of(
                account(1, "USD", "100"),
                account(2, "IDR", "1000000")));

        BalanceSummaryResponse res = service.getBalanceSummary();

        assertThat(res.getBaseCurrency()).isEqualTo("USD");
        assertThat(res.getTotalInBase()).isEqualByComparingTo("163");
        assertThat(res.getByCurrency()).hasSize(2);
    }
}
