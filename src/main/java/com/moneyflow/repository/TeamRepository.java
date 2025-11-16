package com.moneyflow.repository;

import com.moneyflow.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
