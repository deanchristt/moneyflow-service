package com.moneyflow.model.dto;

import com.moneyflow.model.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDTO {
    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private Long ownerId;
    private String ownerName;
    private List<TeamMemberDTO> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TeamDTO from(Team team) {
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .inviteCode(team.getInviteCode())
                .ownerId(team.getOwner().getId())
                .ownerName(team.getOwner().getFirstName() + " " + team.getOwner().getLastName())
                .members(team.getMembers() != null
                        ? team.getMembers().stream()
                        .map(TeamMemberDTO::from)
                        .collect(Collectors.toList())
                        : List.of())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }
}
