package com.hotelaria.dtos.request;

import jakarta.validation.constraints.Size;

public record BookingCancelRequest(
        @Size(max = 255)
        String reason
) {
}
