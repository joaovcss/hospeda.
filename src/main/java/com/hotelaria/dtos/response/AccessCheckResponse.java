package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.AccessDenialReason;

public record AccessCheckResponse(
        Long guestId,
        Long areaId,
        boolean allowed,
        Long bookingId,
        AccessDenialReason denialReason,
        String denialMessage
) {
}
