package com.hotelaria.mappers;

import com.hotelaria.dtos.response.BookingResponse;
import com.hotelaria.models.Booking;
import com.hotelaria.models.Room;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        Room room = booking.getRoom();
        return new BookingResponse(
                booking.getId(),
                booking.getGuest().getId(),
                booking.getGuest().getUser().getFullName(),
                new BookingResponse.RoomInfo(room.getId(), room.getNumber(), room.getFloor(), room.getCategory()),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNights(),
                booking.getNumberOfGuests(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getNotes(),
                booking.getCreatedAt(),
                booking.getConfirmedAt(),
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                booking.getCancelledAt(),
                booking.getCancellationReason());
    }
}
