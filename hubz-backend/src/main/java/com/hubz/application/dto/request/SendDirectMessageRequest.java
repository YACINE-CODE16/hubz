package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendDirectMessageRequest {

    @NotNull(message = "Receiver ID is required")
    private UUID receiverId;

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;
}
