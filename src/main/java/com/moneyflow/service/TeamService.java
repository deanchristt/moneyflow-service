package com.moneyflow.service;

import com.moneyflow.model.dto.CreateTeamRequest;
import com.moneyflow.model.dto.InviteMemberRequest;
import com.moneyflow.model.dto.TeamDTO;
import com.moneyflow.model.dto.TeamMemberDTO;
import com.moneyflow.model.entity.Team;
import com.moneyflow.model.entity.TeamMember;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.TeamRole;
import com.moneyflow.repository.TeamMemberRepository;
import com.moneyflow.repository.TeamRepository;
import com.moneyflow.repository.UserRepository;
import com.moneyflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TeamDTO getMyTeam() {
        Long userId = SecurityUtils.getCurrentUserId();

        // Find team where user is a member
        TeamMember teamMember = teamMemberRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElse(null);

        if (teamMember == null) {
            return null;
        }

        Team team = teamMember.getTeam();
        return TeamDTO.from(team);
    }

    @Transactional
    public TeamDTO createTeam(CreateTeamRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has a team
        if (!teamMemberRepository.findByUserId(userId).isEmpty()) {
            throw new RuntimeException("User already belongs to a team");
        }

        // Generate unique invite code
        String inviteCode = generateInviteCode();

        // Create team
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .inviteCode(inviteCode)
                .owner(user)
                .build();

        team = teamRepository.save(team);

        // Add owner as team member
        TeamMember ownerMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.OWNER)
                .build();

        teamMemberRepository.save(ownerMember);

        return TeamDTO.from(team);
    }

    @Transactional
    public TeamDTO updateTeam(CreateTeamRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Team team = getTeamForUser(userId);

        // Only owner or admin can update team
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Not a team member"));

        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new RuntimeException("Only owner or admin can update team");
        }

        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team = teamRepository.save(team);

        return TeamDTO.from(team);
    }

    @Transactional
    public TeamMemberDTO inviteMember(InviteMemberRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Team team = getTeamForUser(userId);

        // Only owner or admin can invite
        TeamMember inviter = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Not a team member"));

        if (inviter.getRole() != TeamRole.OWNER && inviter.getRole() != TeamRole.ADMIN) {
            throw new RuntimeException("Only owner or admin can invite members");
        }

        // Find user by email
        User newUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with email " + request.getEmail() + " not found"));

        // Check if user is already a member
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), newUser.getId())) {
            throw new RuntimeException("User is already a team member");
        }

        // Cannot make another user OWNER
        TeamRole role = request.getRole();
        if (role == TeamRole.OWNER) {
            throw new RuntimeException("Cannot invite member with OWNER role");
        }

        // Create team member
        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(newUser)
                .role(role)
                .build();

        newMember = teamMemberRepository.save(newMember);

        return TeamMemberDTO.from(newMember);
    }

    @Transactional
    public void removeMember(Long memberUserId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Team team = getTeamForUser(userId);

        // Get current user member
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Not a team member"));

        // Only owner or admin can remove members
        if (currentMember.getRole() != TeamRole.OWNER && currentMember.getRole() != TeamRole.ADMIN) {
            throw new RuntimeException("Only owner or admin can remove members");
        }

        // Get member to remove
        TeamMember memberToRemove = teamMemberRepository.findByTeamIdAndUserId(team.getId(), memberUserId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Cannot remove owner
        if (memberToRemove.getRole() == TeamRole.OWNER) {
            throw new RuntimeException("Cannot remove team owner");
        }

        // Admin cannot remove other admins or owner
        if (currentMember.getRole() == TeamRole.ADMIN &&
                (memberToRemove.getRole() == TeamRole.ADMIN || memberToRemove.getRole() == TeamRole.OWNER)) {
            throw new RuntimeException("Admin cannot remove other admins or owner");
        }

        teamMemberRepository.delete(memberToRemove);
    }

    @Transactional
    public TeamMemberDTO updateMemberRole(Long memberUserId, TeamRole newRole) {
        Long userId = SecurityUtils.getCurrentUserId();
        Team team = getTeamForUser(userId);

        // Only owner can change roles
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                .orElseThrow(() -> new RuntimeException("Not a team member"));

        if (currentMember.getRole() != TeamRole.OWNER) {
            throw new RuntimeException("Only owner can change member roles");
        }

        // Get member to update
        TeamMember memberToUpdate = teamMemberRepository.findByTeamIdAndUserId(team.getId(), memberUserId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Cannot change owner role
        if (memberToUpdate.getRole() == TeamRole.OWNER || newRole == TeamRole.OWNER) {
            throw new RuntimeException("Cannot change owner role");
        }

        memberToUpdate.setRole(newRole);
        memberToUpdate = teamMemberRepository.save(memberToUpdate);

        return TeamMemberDTO.from(memberToUpdate);
    }

    private Team getTeamForUser(Long userId) {
        TeamMember member = teamMemberRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User does not belong to any team"));

        return member.getTeam();
    }

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (teamRepository.existsByInviteCode(code));
        return code;
    }
}
