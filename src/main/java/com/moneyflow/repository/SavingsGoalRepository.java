package com.moneyflow.repository;

import com.moneyflow.model.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdAndIsActiveTrue(Long userId);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);
}
