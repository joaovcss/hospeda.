package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.RoomCategory;
import com.hotelaria.models.enums.RoomStatus;

import java.math.BigDecimal;

public record RoomResponse(
        Long id,
        String number,
        Integer floor,
        Integer capacity,
        RoomCategory category,
        BigDecimal dailyRate,
        RoomStatus status,
        String description
) {
}
