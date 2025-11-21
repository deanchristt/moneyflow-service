package com.moneyflow.controller;

import com.moneyflow.model.dto.*;
import com.moneyflow.model.enums.TeamRole;
import com.moneyflow.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "Team management endpoints")
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/my-team")
    @Operation(summary = "Get current user's team")
    public ResponseEntity<ApiResponse<TeamDTO>> getMyTeam() {
        TeamDTO team = teamService.getMyTeam();
        if (team == null) {
            return ResponseEntity.ok(ApiResponse.success("User does not belong to any team", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Team retrieved successfully", team));
    }

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<ApiResponse<TeamDTO>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamDTO team = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Team created successfully", team));
    }

    @PutMapping
    @Operation(summary = "Update team details")
    public ResponseEntity<ApiResponse<TeamDTO>> updateTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamDTO team = teamService.updateTeam(request);
        return ResponseEntity.ok(ApiResponse.success("Team updated successfully", team));
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite a member to the team")
    public ResponseEntity<ApiResponse<TeamMemberDTO>> inviteMember(@Valid @RequestBody InviteMemberRequest request) {
        TeamMemberDTO member = teamService.inviteMember(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member invited successfully", member));
    }

    @DeleteMapping("/members/{userId}")
    @Operation(summary = "Remove a member from the team")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long userId) {
        teamService.removeMember(userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    @PutMapping("/members/{userId}/role")
    @Operation(summary = "Update member role")
    public ResponseEntity<ApiResponse<TeamMemberDTO>> updateMemberRole(
            @PathVariable Long userId,
            @RequestBody UpdateMemberRoleRequest request) {
        TeamMemberDTO member = teamService.updateMemberRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("Member role updated successfully", member));
    }

    @Data
    static class UpdateMemberRoleRequest {
        private TeamRole role;
    }
}
