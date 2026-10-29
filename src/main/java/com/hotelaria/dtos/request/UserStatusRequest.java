package com.hotelaria.dtos.request;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull
        Boolean active
) {
}
