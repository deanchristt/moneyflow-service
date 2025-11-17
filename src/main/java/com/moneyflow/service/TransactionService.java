package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.transaction.CreateTransactionRequest;
import com.moneyflow.model.dto.transaction.TransactionFilterRequest;
import com.moneyflow.model.dto.transaction.TransactionResponse;
import com.moneyflow.model.dto.transaction.UpdateTransactionRequest;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.CategoryRepository;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        Category category = categoryRepository.findById(request.getCategoryId())
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
            transferToAccount = accountRepository.findByIdAndUserId(request.getTransferToAccountId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getTransferToAccountId()));
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

        // Update account balances
        updateAccountBalance(account, request.getType(), request.getAmount(), true);
        if (transferToAccount != null) {
            // Add to destination account
            transferToAccount.setBalance(transferToAccount.getBalance().add(request.getAmount()));
            accountRepository.save(transferToAccount);
        }

        transaction = transactionRepository.save(transaction);
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

        return transactionRepository.findByFilters(
                userId,
                filter.getAccountId(),
                filter.getCategoryId(),
                filter.getType(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        // If amount changes, adjust account balance
        if (request.getAmount() != null && !request.getAmount().equals(transaction.getAmount())) {
            BigDecimal difference = request.getAmount().subtract(transaction.getAmount());

            // Reverse old amount effect and apply new
            Account account = transaction.getAccount();
            if (transaction.getType() == TransactionType.INCOME) {
                account.setBalance(account.getBalance().add(difference));
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                account.setBalance(account.getBalance().subtract(difference));
            } else if (transaction.getType() == TransactionType.TRANSFER) {
                account.setBalance(account.getBalance().subtract(difference));
                if (transaction.getTransferToAccount() != null) {
                    Account toAccount = transaction.getTransferToAccount();
                    toAccount.setBalance(toAccount.getBalance().add(difference));
                    accountRepository.save(toAccount);
                }
            }
            accountRepository.save(account);
            transaction.setAmount(request.getAmount());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
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

        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        // Reverse the balance effect
        Account account = transaction.getAccount();
        updateAccountBalance(account, transaction.getType(), transaction.getAmount(), false);

        if (transaction.getType() == TransactionType.TRANSFER && transaction.getTransferToAccount() != null) {
            Account toAccount = transaction.getTransferToAccount();
            toAccount.setBalance(toAccount.getBalance().subtract(transaction.getAmount()));
            accountRepository.save(toAccount);
        }

        // Soft delete
        transaction.setIsActive(false);
        transactionRepository.save(transaction);
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

    private void updateAccountBalance(Account account, TransactionType type, BigDecimal amount, boolean isCreating) {
        BigDecimal balanceChange = isCreating ? amount : amount.negate();

        switch (type) {
            case INCOME -> account.setBalance(account.getBalance().add(balanceChange));
            case EXPENSE, TRANSFER -> account.setBalance(account.getBalance().subtract(balanceChange));
        }

        accountRepository.save(account);
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
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
