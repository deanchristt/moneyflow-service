package com.moneyflow.repository;

import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"account", "category"})
    List<Transaction> findByAccountIdInAndTransactionDateBetweenAndIsActiveTrue(
            List<Long> accountIds, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndAccountIdAndTransactionDateBetweenAndIsActiveTrue(
            Long userId, Long accountId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByAccountIdAndIsActiveTrue(Long accountId);

    List<Transaction> findByCategoryIdAndIsActiveTrue(Long categoryId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"account", "category", "transferToAccount"})
    @Query("SELECT t FROM Transaction t WHERE t.account.id IN :accountIds " +
            "AND (:accountId IS NULL OR t.account.id = :accountId) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
            "AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(t.note) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:tagId IS NULL OR :tagId IN (SELECT tg.id FROM t.tags tg)) " +
            "AND t.isActive = true")
    Page<Transaction> findByFilters(
            @Param("accountIds") List<Long> accountIds,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            @Param("tagId") Long tagId,
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

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.category.id = :categoryId AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.isActive = true")
    BigDecimal sumAmountByCategoryTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"account"})
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId " +
            "AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isActive = true")
    List<Transaction> findSpentTransactionsForUser(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"account"})
    @Query("SELECT t FROM Transaction t WHERE t.category.id = :categoryId " +
            "AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isActive = true")
    List<Transaction> findSpentTransactionsForCategory(
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.category.id, t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.category.id IN :categoryIds " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isActive = true " +
            "GROUP BY t.category.id, t.type")
    List<Object[]> sumByCategoriesAndPeriodGrouped(
            @Param("userId") Long userId,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
