package com.hubz.application.dto.request;

import com.hubz.domain.enums.ParticipantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondEventInvitationRequest {
    @NotNull(message = "Status is required (ACCEPTED or DECLINED)")
    private ParticipantStatus status;
}
