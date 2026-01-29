package com.hubz.application.dto.request;

import com.hubz.domain.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private MemberRole role;
}
