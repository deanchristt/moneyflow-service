package com.moneyflow.repository;

import com.moneyflow.model.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByUserId(Long userId);

    List<TeamMember> findByTeamId(Long teamId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);
}
