package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.AccessDenialReason;
import com.hotelaria.models.enums.AccessResult;

import java.time.LocalDateTime;

public record AccessLogResponse(
        Long id,
        Long guestId,
        String guestName,
        Long bookingId,
        Long areaId,
        String areaName,
        LocalDateTime attemptedAt,
        AccessResult result,
        AccessDenialReason denialReason,
        String denialMessage,
        Long registeredByUserId,
        String registeredByName
) {
}
