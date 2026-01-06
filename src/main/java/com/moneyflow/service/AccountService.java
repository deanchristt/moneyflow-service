package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.account.AccountResponse;
import com.moneyflow.model.dto.account.BalanceSummaryResponse;
import com.moneyflow.model.dto.account.CreateAccountRequest;
import com.moneyflow.model.dto.account.UpdateAccountRequest;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.TeamMember;
import com.moneyflow.model.entity.User;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TeamPermissionService teamPermissionService;
    private final CurrencyService currencyService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (accountRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new BadRequestException("Account with this name already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = Account.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .icon(request.getIcon())
                .color(request.getColor())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        // If this is set as default, remove default from other accounts
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            removeDefaultFromOtherAccounts(userId);
        }

        account = accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        Long userId = SecurityUtils.getCurrentUserId();
        // Own accounts plus accounts shared with the user's team.
        return accountRepository.findAllAccessibleByUser(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Account account = accountRepository.findById(id)
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .filter(a -> teamPermissionService.canAccessAccount(userId, a))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse shareAccount(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        TeamMember membership = teamPermissionService.membership(userId)
                .orElseThrow(() -> new BadRequestException("You must belong to a team to share an account"));
        account.setTeam(membership.getTeam());
        account = accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse unshareAccount(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
        account.setTeam(null);
        account = accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        if (request.getName() != null && !request.getName().equals(account.getName())) {
            if (accountRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new BadRequestException("Account with this name already exists");
            }
            account.setName(request.getName());
        }

        if (request.getType() != null) {
            account.setType(request.getType());
        }

        if (request.getIcon() != null) {
            account.setIcon(request.getIcon());
        }

        if (request.getColor() != null) {
            account.setColor(request.getColor());
        }

        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                removeDefaultFromOtherAccounts(userId);
            }
            account.setIsDefault(request.getIsDefault());
        }

        account = accountRepository.save(account);
        return mapToResponse(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        // Soft delete
        account.setIsActive(false);
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance() {
        Long userId = SecurityUtils.getCurrentUserId();
        // Convert each account to the base currency before summing (accounts may differ in currency).
        return accountRepository.findAllAccessibleByUser(userId)
                .stream()
                .map(a -> currencyService.toBase(a.getBalance(), a.getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BalanceSummaryResponse getBalanceSummary() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Account> accounts = accountRepository.findAllAccessibleByUser(userId);

        Map<String, BigDecimal> totalsByCurrency = new LinkedHashMap<>();
        for (Account a : accounts) {
            totalsByCurrency.merge(a.getCurrency(), a.getBalance(), BigDecimal::add);
        }

        List<BalanceSummaryResponse.CurrencyBalance> byCurrency = new ArrayList<>();
        BigDecimal totalInBase = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : totalsByCurrency.entrySet()) {
            BigDecimal inBase = currencyService.toBase(e.getValue(), e.getKey());
            totalInBase = totalInBase.add(inBase);
            byCurrency.add(BalanceSummaryResponse.CurrencyBalance.builder()
                    .currency(e.getKey())
                    .total(e.getValue())
                    .totalInBase(inBase)
                    .build());
        }

        return BalanceSummaryResponse.builder()
                .baseCurrency(currencyService.getBaseCurrency())
                .totalInBase(totalInBase)
                .byCurrency(byCurrency)
                .build();
    }

    private void removeDefaultFromOtherAccounts(Long userId) {
        accountRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(defaultAccount -> {
                    defaultAccount.setIsDefault(false);
                    accountRepository.save(defaultAccount);
                });
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .icon(account.getIcon())
                .color(account.getColor())
                .isDefault(account.getIsDefault())
                .teamId(account.getTeam() != null ? account.getTeam().getId() : null)
                .teamName(account.getTeam() != null ? account.getTeam().getName() : null)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
