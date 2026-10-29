package com.hotelaria.dtos.request;

import jakarta.validation.constraints.NotNull;

public record AccessValidationRequest(
        @NotNull
        Long guestId,

        @NotNull
        Long areaId
) {
}
