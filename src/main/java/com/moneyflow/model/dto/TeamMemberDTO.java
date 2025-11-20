package com.moneyflow.model.dto;

import com.moneyflow.model.entity.TeamMember;
import com.moneyflow.model.enums.TeamRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDTO {
    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private TeamRole role;
    private LocalDateTime joinedAt;

    public static TeamMemberDTO from(TeamMember member) {
        return TeamMemberDTO.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .firstName(member.getUser().getFirstName())
                .lastName(member.getUser().getLastName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
