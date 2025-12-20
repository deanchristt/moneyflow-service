package com.moneyflow.service;

import com.moneyflow.exception.UnauthorizedException;
import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Team;
import com.moneyflow.model.entity.TeamMember;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.TeamRole;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.TeamMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamPermissionServiceTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private AccountRepository accountRepository;

    @InjectMocks private TeamPermissionService service;

    private User user(long id) {
        User u = User.builder().email("u" + id + "@ex.com").build();
        u.setId(id);
        return u;
    }

    private Account account(long id, long ownerId, Team team) {
        Account a = Account.builder().name("acc" + id).user(user(ownerId)).team(team).build();
        a.setId(id);
        return a;
    }

    private void memberInTeam(long userId, long teamId, TeamRole role) {
        Team team = Team.builder().name("Team").build();
        team.setId(teamId);
        TeamMember m = TeamMember.builder().team(team).user(user(userId)).role(role).build();
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of(m));
    }

    @Test
    void ownerCanAccessOwnAccount() {
        when(teamMemberRepository.findByUserId(1L)).thenReturn(List.of());
        assertThat(service.canAccessAccount(1L, account(10, 1, null))).isTrue();
    }

    @Test
    void canAccessTeamSharedAccount() {
        Team team = Team.builder().name("Fam").build();
        team.setId(99L);
        memberInTeam(1L, 99L, TeamRole.MEMBER);
        // account owned by user 2 but shared with team 99
        assertThat(service.canAccessAccount(1L, account(10, 2, team))).isTrue();
    }

    @Test
    void cannotAccessOtherUsersPrivateAccount() {
        when(teamMemberRepository.findByUserId(1L)).thenReturn(List.of());
        assertThat(service.canAccessAccount(1L, account(10, 2, null))).isFalse();
    }

    @Test
    void viewerCannotWrite() {
        memberInTeam(1L, 99L, TeamRole.VIEWER);
        assertThatThrownBy(() -> service.assertCanWrite(1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void memberCanWriteAndUserWithoutTeamCanWrite() {
        memberInTeam(1L, 99L, TeamRole.MEMBER);
        assertThatCode(() -> service.assertCanWrite(1L)).doesNotThrowAnyException();

        when(teamMemberRepository.findByUserId(2L)).thenReturn(List.of());
        assertThatCode(() -> service.assertCanWrite(2L)).doesNotThrowAnyException();
    }
}
