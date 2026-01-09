package com.moneyflow.repository;

import com.moneyflow.model.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndMonthAndYearAndIsActiveTrue(Long userId, Integer month, Integer year);

    List<Budget> findByUserIdAndYearAndIsActiveTrue(Long userId, Integer year);

    @Query("SELECT b FROM Budget b WHERE (b.user.id = :userId " +
            "OR b.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)) " +
            "AND b.month = :month AND b.year = :year AND b.isActive = true")
    List<Budget> findVisibleByMonthAndYear(
            @Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT b FROM Budget b WHERE (b.user.id = :userId " +
            "OR b.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)) " +
            "AND b.year = :year AND b.isActive = true")
    List<Budget> findVisibleByYear(@Param("userId") Long userId, @Param("year") Integer year);

    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
            Long userId, Long categoryId, Integer month, Integer year);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryIdAndMonthAndYear(
            Long userId, Long categoryId, Integer month, Integer year);
}
