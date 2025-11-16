package com.moneyflow.repository;

import com.moneyflow.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserIdAndIsActiveTrue(Long userId);

    List<Account> findByTeamIdAndIsActiveTrue(Long teamId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    Optional<Account> findByUserIdAndIsDefaultTrue(Long userId);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId OR a.team.id IN " +
            "(SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)")
    List<Account> findAllAccessibleByUser(@Param("userId") Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
