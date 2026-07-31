package com.moneyflow.repository;

import com.moneyflow.model.entity.Category;
import com.moneyflow.model.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdAndIsActiveTrue(Long userId);

    List<Category> findByUserIdAndTypeAndIsActiveTrue(Long userId, CategoryType type);

    List<Category> findByIsDefaultTrueAndIsActiveTrue();

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.isDefault = true " +
            "OR c.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)) AND c.isActive = true")
    List<Category> findAllAvailableForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.isDefault = true " +
            "OR c.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)) " +
            "AND c.type = :type AND c.isActive = true")
    List<Category> findAllAvailableForUserByType(@Param("userId") Long userId, @Param("type") CategoryType type);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user.id = :userId OR c.isDefault = true " +
            "OR c.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)) AND c.isActive = true")
    Optional<Category> findByIdAndAvailableForUser(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
