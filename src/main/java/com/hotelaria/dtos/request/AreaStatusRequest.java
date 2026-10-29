package com.hotelaria.dtos.request;

import com.hotelaria.models.enums.AreaStatus;
import jakarta.validation.constraints.NotNull;

public record AreaStatusRequest(
        @NotNull
        AreaStatus status
) {
}
