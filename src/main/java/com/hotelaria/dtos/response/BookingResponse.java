package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.RoomCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long guestId,
        String guestName,
        RoomInfo room,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        Integer numberOfGuests,
        BookingStatus status,
        BigDecimal totalAmount,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        LocalDateTime checkedInAt,
        LocalDateTime checkedOutAt,
        LocalDateTime cancelledAt,
        String cancellationReason
) {

    public record RoomInfo(
            Long id,
            String number,
            Integer floor,
            RoomCategory category
    ) {
    }
}
