package com.moneyflow.service;

import com.moneyflow.exception.UnauthorizedException;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.TeamMember;
import com.moneyflow.model.enums.TeamRole;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central place for team-based access rules. A user belongs to at most one team.
 * "Accessible" data = the user's own plus accounts shared with their team.
 * Roles: VIEWER = read-only; MEMBER/ADMIN/OWNER = read+write on accessible data.
 */
@Service
@RequiredArgsConstructor
public class TeamPermissionService {

    private final TeamMemberRepository teamMemberRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public Optional<TeamMember> membership(Long userId) {
        return teamMemberRepository.findByUserId(userId).stream().findFirst();
    }

    public Long teamId(Long userId) {
        return membership(userId).map(m -> m.getTeam().getId()).orElse(null);
    }

    public TeamRole role(Long userId) {
        return membership(userId).map(TeamMember::getRole).orElse(null);
    }

    /** VIEWERs may not create/modify data. Users without a team are unrestricted. */
    public void assertCanWrite(Long userId) {
        if (role(userId) == TeamRole.VIEWER) {
            throw new UnauthorizedException("Viewers have read-only access to shared data");
        }
    }

    public boolean canAccessAccount(Long userId, Account account) {
        if (account.getUser().getId().equals(userId)) {
            return true;
        }
        Long teamId = teamId(userId);
        return account.getTeam() != null && teamId != null
                && account.getTeam().getId().equals(teamId);
    }

    @Transactional(readOnly = true)
    public List<Long> accessibleAccountIds(Long userId) {
        return accountRepository.findAllAccessibleByUser(userId).stream()
                .map(Account::getId)
                .collect(Collectors.toList());
    }
}
