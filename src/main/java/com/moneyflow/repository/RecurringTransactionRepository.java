package com.moneyflow.repository;

import com.moneyflow.model.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserIdAndIsActiveTrue(Long userId);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.nextExecutionDate <= :date " +
            "AND rt.isPaused = false AND rt.isActive = true " +
            "AND (rt.endDate IS NULL OR rt.endDate >= :date)")
    List<RecurringTransaction> findDueRecurringTransactions(@Param("date") LocalDate date);

    List<RecurringTransaction> findByUserIdAndIsPausedFalseAndIsActiveTrue(Long userId);

    List<RecurringTransaction> findByAccountIdAndIsActiveTrue(Long accountId);
}
