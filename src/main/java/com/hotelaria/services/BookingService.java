package com.hotelaria.services;

import com.hotelaria.dtos.request.BookingCreateRequest;
import com.hotelaria.dtos.request.BookingScope;
import com.hotelaria.dtos.response.BookingResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.models.enums.BookingStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BookingService {

    BookingResponse create(BookingCreateRequest request);

    PageResponse<BookingResponse> list(Long guestId, Long roomId, BookingStatus status,
                                       LocalDate from, LocalDate to, Pageable pageable);

    BookingResponse findById(Long id);

    PageResponse<BookingResponse> listMine(BookingScope scope, Pageable pageable);

    BookingResponse confirm(Long id);

    BookingResponse checkIn(Long id);

    BookingResponse checkOut(Long id);

    BookingResponse cancel(Long id, String reason);
}
