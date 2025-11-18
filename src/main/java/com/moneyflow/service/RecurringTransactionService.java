package com.moneyflow.service;

import com.moneyflow.exception.BadRequestException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.model.dto.recurring.CreateRecurringTransactionRequest;
import com.moneyflow.model.dto.recurring.RecurringTransactionResponse;
import com.moneyflow.model.dto.recurring.UpdateRecurringTransactionRequest;
import com.moneyflow.model.entity.*;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.*;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(CreateRecurringTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // Transfers not supported for recurring
        if (request.getType() == TransactionType.TRANSFER) {
            throw new BadRequestException("Transfer type is not supported for recurring transactions");
        }

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription() != null ? request.getDescription() : category.getName())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextExecutionDate(request.getStartDate())
                .build();

        recurringTransaction = recurringTransactionRepository.save(recurringTransaction);
        return mapToResponse(recurringTransaction);
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getAllRecurringTransactions() {
        Long userId = SecurityUtils.getCurrentUserId();
        return recurringTransactionRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getActiveRecurringTransactions() {
        Long userId = SecurityUtils.getCurrentUserId();
        return recurringTransactionRepository.findByUserIdAndIsPausedFalseAndIsActiveTrue(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", "id", id));
        return mapToResponse(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(Long id, UpdateRecurringTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", "id", id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            recurringTransaction.setCategory(category);
        }

        if (request.getAmount() != null) {
            recurringTransaction.setAmount(request.getAmount());
        }

        if (request.getDescription() != null) {
            recurringTransaction.setDescription(request.getDescription());
        }

        if (request.getFrequency() != null) {
            recurringTransaction.setFrequency(request.getFrequency());
        }

        if (request.getEndDate() != null) {
            recurringTransaction.setEndDate(request.getEndDate());
        }

        if (request.getIsPaused() != null) {
            recurringTransaction.setIsPaused(request.getIsPaused());
        }

        recurringTransaction = recurringTransactionRepository.save(recurringTransaction);
        return mapToResponse(recurringTransaction);
    }

    @Transactional
    public void deleteRecurringTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", "id", id));

        // Soft delete
        recurringTransaction.setIsActive(false);
        recurringTransactionRepository.save(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse pauseRecurringTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", "id", id));

        recurringTransaction.setIsPaused(true);
        recurringTransaction = recurringTransactionRepository.save(recurringTransaction);
        return mapToResponse(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse resumeRecurringTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", "id", id));

        recurringTransaction.setIsPaused(false);
        recurringTransaction = recurringTransactionRepository.save(recurringTransaction);
        return mapToResponse(recurringTransaction);
    }

    /**
     * Execute a single recurring transaction manually
     */
    @Transactional
    public void executeRecurringTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", "id", id));

        createTransactionFromRecurring(recurringTransaction);
    }

    /**
     * Process all due recurring transactions (for scheduled job)
     */
    @Transactional
    public int processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueTransactions = recurringTransactionRepository.findDueRecurringTransactions(today);

        int count = 0;
        for (RecurringTransaction recurring : dueTransactions) {
            try {
                createTransactionFromRecurring(recurring);
                count++;
            } catch (Exception e) {
                log.error("Failed to process recurring transaction {}: {}", recurring.getId(), e.getMessage());
            }
        }

        return count;
    }

    private void createTransactionFromRecurring(RecurringTransaction recurring) {
        Account account = recurring.getAccount();

        // Create the transaction
        Transaction transaction = Transaction.builder()
                .user(recurring.getUser())
                .account(account)
                .category(recurring.getCategory())
                .type(recurring.getType())
                .amount(recurring.getAmount())
                .description(recurring.getDescription())
                .transactionDate(recurring.getNextExecutionDate())
                .recurringTransaction(recurring)
                .build();

        // Update account balance
        if (recurring.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(recurring.getAmount()));
        } else if (recurring.getType() == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(recurring.getAmount()));
        }

        accountRepository.save(account);
        transactionRepository.save(transaction);

        // Update recurring transaction
        recurring.setLastExecutedAt(LocalDateTime.now());
        recurring.setNextExecutionDate(recurring.calculateNextExecutionDate());
        recurringTransactionRepository.save(recurring);
    }

    private RecurringTransactionResponse mapToResponse(RecurringTransaction recurring) {
        int totalExecutions = recurring.getGeneratedTransactions() != null
                ? recurring.getGeneratedTransactions().size()
                : 0;

        return RecurringTransactionResponse.builder()
                .id(recurring.getId())
                .type(recurring.getType())
                .amount(recurring.getAmount())
                .description(recurring.getDescription())
                .frequency(recurring.getFrequency())
                .startDate(recurring.getStartDate())
                .endDate(recurring.getEndDate())
                .nextExecutionDate(recurring.getNextExecutionDate())
                .lastExecutedAt(recurring.getLastExecutedAt())
                .isPaused(recurring.getIsPaused())
                .accountId(recurring.getAccount().getId())
                .accountName(recurring.getAccount().getName())
                .categoryId(recurring.getCategory().getId())
                .categoryName(recurring.getCategory().getName())
                .categoryIcon(recurring.getCategory().getIcon())
                .categoryColor(recurring.getCategory().getColor())
                .totalExecutions(totalExecutions)
                .createdAt(recurring.getCreatedAt())
                .updatedAt(recurring.getUpdatedAt())
                .build();
    }
}
