package com.hotelaria.dtos.request;

import com.hotelaria.models.enums.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RoomStatusRequest(
        @Schema(description = "AVAILABLE, MAINTENANCE ou INACTIVE (OCCUPIED é definido pelo check-in)")
        @NotNull
        RoomStatus status
) {
}
