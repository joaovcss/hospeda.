package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.AccessGrantStatus;

import java.time.LocalDateTime;

public record BookingAreaAccessResponse(
        Long id,
        Long bookingId,
        Long areaId,
        String areaName,
        String areaLocation,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        AccessGrantStatus status,
        LocalDateTime createdAt,
        LocalDateTime revokedAt
) {
}
