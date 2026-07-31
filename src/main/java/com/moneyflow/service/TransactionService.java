package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.transaction.CreateTransactionRequest;
import com.moneyflow.model.dto.transaction.TransactionFilterRequest;
import com.moneyflow.model.dto.transaction.TransactionResponse;
import com.moneyflow.model.dto.transaction.UpdateTransactionRequest;
import com.moneyflow.model.dto.tag.TagResponse;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.Tag;
import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.AccountType;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.CategoryRepository;
import com.moneyflow.repository.TagRepository;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final BudgetAlertService budgetAlertService;
    private final TeamPermissionService teamPermissionService;

    /** When true, non-credit accounts may not be driven below zero. Off by default. */
    @Value("${moneyflow.accounts.enforce-sufficient-balance:false}")
    private boolean enforceSufficientBalance;

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        teamPermissionService.assertCanWrite(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = resolveAccessibleAccount(request.getAccountId(), userId);

        Category category = categoryRepository.findByIdAndAvailableForUser(request.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // Validate transfer
        Account transferToAccount = null;
        if (request.getType() == TransactionType.TRANSFER) {
            if (request.getTransferToAccountId() == null) {
                throw new BadRequestException("Transfer destination account is required");
            }
            if (request.getTransferToAccountId().equals(request.getAccountId())) {
                throw new BadRequestException("Cannot transfer to the same account");
            }
            transferToAccount = resolveAccessibleAccount(request.getTransferToAccountId(), userId);
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription() != null ? request.getDescription() : category.getName())
                .note(request.getNote())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now())
                .referenceNumber(request.getReferenceNumber())
                .transferToAccount(transferToAccount)
                .build();

        if (request.getTagIds() != null) {
            transaction.setTags(resolveTags(request.getTagIds(), userId));
        }

        // Update account balances (with insufficient-funds guard)
        applyTransactionEffect(account, request.getType(), request.getAmount(), transferToAccount);

        transaction = transactionRepository.save(transaction);

        if (transaction.getType() == TransactionType.EXPENSE) {
            budgetAlertService.evaluateForCategory(userId, category.getId(), transaction.getTransactionDate());
        }

        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter) {
        Long userId = SecurityUtils.getCurrentUserId();

        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getSize() != null ? filter.getSize() : 20;
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "transactionDate";
        String sortDir = filter.getSortDirection() != null ? filter.getSortDirection() : "desc";

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        List<Long> accessibleAccountIds = teamPermissionService.accessibleAccountIds(userId);
        if (accessibleAccountIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return transactionRepository.findByFilters(
                accessibleAccountIds,
                filter.getAccountId(),
                filter.getCategoryId(),
                filter.getType(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getSearch(),
                filter.getTagId(),
                pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Transaction transaction = transactionRepository.findById(id)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .filter(t -> teamPermissionService.canAccessAccount(userId, t.getAccount()))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        teamPermissionService.assertCanWrite(userId);

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        Long previousCategoryId = transaction.getCategory().getId();
        LocalDate previousDate = transaction.getTransactionDate();

        // Snapshot old monetary effect
        Account oldAccount = transaction.getAccount();
        TransactionType oldType = transaction.getType();
        BigDecimal oldAmount = transaction.getAmount();
        Account oldTransferTo = transaction.getTransferToAccount();

        // Resolve new monetary values (fall back to current when not provided)
        TransactionType newType = request.getType() != null ? request.getType() : oldType;
        BigDecimal newAmount = request.getAmount() != null ? request.getAmount() : oldAmount;
        Account newAccount = oldAccount;
        if (request.getAccountId() != null && !request.getAccountId().equals(oldAccount.getId())) {
            newAccount = resolveAccessibleAccount(request.getAccountId(), userId);
        }

        Account newTransferTo = null;
        if (newType == TransactionType.TRANSFER) {
            Long transferToId = request.getTransferToAccountId() != null
                    ? request.getTransferToAccountId()
                    : (oldTransferTo != null ? oldTransferTo.getId() : null);
            if (transferToId == null) {
                throw new BadRequestException("Transfer destination account is required");
            }
            if (transferToId.equals(newAccount.getId())) {
                throw new BadRequestException("Cannot transfer to the same account");
            }
            newTransferTo = resolveAccessibleAccount(transferToId, userId);
        }

        // Reverse the old effect, then apply the new one (handles any account/type/amount change)
        reverseTransactionEffect(oldAccount, oldType, oldAmount, oldTransferTo);
        applyTransactionEffect(newAccount, newType, newAmount, newTransferTo);

        transaction.setAccount(newAccount);
        transaction.setType(newType);
        transaction.setAmount(newAmount);
        transaction.setTransferToAccount(newTransferTo);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndAvailableForUser(request.getCategoryId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            transaction.setCategory(category);
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        if (request.getNote() != null) {
            transaction.setNote(request.getNote());
        }

        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }

        if (request.getReferenceNumber() != null) {
            transaction.setReferenceNumber(request.getReferenceNumber());
        }

        if (request.getTagIds() != null) {
            transaction.setTags(resolveTags(request.getTagIds(), userId));
        }

        transaction = transactionRepository.save(transaction);

        if (transaction.getType() == TransactionType.EXPENSE) {
            budgetAlertService.evaluateForCategory(
                    userId, transaction.getCategory().getId(), transaction.getTransactionDate());
            // The spending on the previous category/period changed too; re-evaluate it.
            if (!previousCategoryId.equals(transaction.getCategory().getId())
                    || !previousDate.equals(transaction.getTransactionDate())) {
                budgetAlertService.evaluateForCategory(userId, previousCategoryId, previousDate);
            }
        }

        return mapToResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        teamPermissionService.assertCanWrite(userId);

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        // Reverse the balance effect
        reverseTransactionEffect(transaction.getAccount(), transaction.getType(),
                transaction.getAmount(), transaction.getTransferToAccount());

        // Soft delete
        transaction.setIsActive(false);
        transactionRepository.save(transaction);

        if (transaction.getType() == TransactionType.EXPENSE) {
            budgetAlertService.evaluateForCategory(
                    userId, transaction.getCategory().getId(), transaction.getTransactionDate());
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalIncome(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        BigDecimal total = transactionRepository.sumAmountByTypeAndDateRange(
                userId, TransactionType.INCOME, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpense(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        BigDecimal total = transactionRepository.sumAmountByTypeAndDateRange(
                userId, TransactionType.EXPENSE, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    /** Apply the monetary effect of a transaction to account balances, guarding against overdraft. */
    private void applyTransactionEffect(Account source, TransactionType type, BigDecimal amount, Account transferTo) {
        switch (type) {
            case INCOME -> source.setBalance(source.getBalance().add(amount));
            case EXPENSE -> {
                guardSufficientBalance(source, amount);
                source.setBalance(source.getBalance().subtract(amount));
            }
            case TRANSFER -> {
                guardSufficientBalance(source, amount);
                source.setBalance(source.getBalance().subtract(amount));
                if (transferTo != null) {
                    transferTo.setBalance(transferTo.getBalance().add(amount));
                }
            }
        }
        accountRepository.save(source);
        if (type == TransactionType.TRANSFER && transferTo != null) {
            accountRepository.save(transferTo);
        }
    }

    /** Reverse a previously applied monetary effect (no overdraft guard — reversals only free up funds). */
    private void reverseTransactionEffect(Account source, TransactionType type, BigDecimal amount, Account transferTo) {
        switch (type) {
            case INCOME -> source.setBalance(source.getBalance().subtract(amount));
            case EXPENSE -> source.setBalance(source.getBalance().add(amount));
            case TRANSFER -> {
                source.setBalance(source.getBalance().add(amount));
                if (transferTo != null) {
                    transferTo.setBalance(transferTo.getBalance().subtract(amount));
                }
            }
        }
        accountRepository.save(source);
        if (type == TransactionType.TRANSFER && transferTo != null) {
            accountRepository.save(transferTo);
        }
    }

    /** Resolve an account the user may transact on (own or shared with their team). */
    private Account resolveAccessibleAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        if (!teamPermissionService.canAccessAccount(userId, account)) {
            throw new ResourceNotFoundException("Account", "id", accountId);
        }
        return account;
    }

    /** Resolve tag ids to the user's own tags, rejecting any that don't belong to them. */
    private Set<Tag> resolveTags(Set<Long> tagIds, Long userId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> tags = tagRepository.findByUserIdAndIdIn(userId, tagIds);
        if (tags.size() != tagIds.size()) {
            throw new BadRequestException("One or more tags were not found");
        }
        return new HashSet<>(tags);
    }

    /** Non-credit accounts (cash, bank, e-wallet) may not go negative when enforcement is enabled. */
    private void guardSufficientBalance(Account account, BigDecimal deduction) {
        if (enforceSufficientBalance
                && account.getType() != AccountType.CREDIT_CARD
                && account.getBalance().subtract(deduction).compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Insufficient balance in account '" + account.getName() + "'");
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .note(transaction.getNote())
                .transactionDate(transaction.getTransactionDate())
                .referenceNumber(transaction.getReferenceNumber())
                .accountId(transaction.getAccount().getId())
                .accountName(transaction.getAccount().getName())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getName())
                .categoryIcon(transaction.getCategory().getIcon())
                .categoryColor(transaction.getCategory().getColor())
                .transferToAccountId(transaction.getTransferToAccount() != null
                        ? transaction.getTransferToAccount().getId() : null)
                .transferToAccountName(transaction.getTransferToAccount() != null
                        ? transaction.getTransferToAccount().getName() : null)
                .recurringTransactionId(transaction.getRecurringTransaction() != null
                        ? transaction.getRecurringTransaction().getId() : null)
                .tags(transaction.getTags() == null ? List.of() : transaction.getTags().stream()
                        .map(t -> TagResponse.builder().id(t.getId()).name(t.getName()).color(t.getColor()).build())
                        .collect(Collectors.toList()))
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
