package com.moneyflow.repository;

import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionDateBetweenAndIsActiveTrue(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndAccountIdAndTransactionDateBetweenAndIsActiveTrue(
            Long userId, Long accountId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByAccountIdAndIsActiveTrue(Long accountId);

    List<Transaction> findByCategoryIdAndIsActiveTrue(Long categoryId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
            "AND (:accountId IS NULL OR t.account.id = :accountId) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
            "AND t.isActive = true")
    Page<Transaction> findByFilters(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.isActive = true")
    BigDecimal sumAmountByTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.category.id = :categoryId AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.isActive = true")
    BigDecimal sumAmountByCategoryAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
