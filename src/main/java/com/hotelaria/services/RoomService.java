package com.hotelaria.services;

import com.hotelaria.dtos.request.RoomRequest;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.RoomResponse;
import com.hotelaria.models.enums.RoomStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RoomService {

    RoomResponse create(RoomRequest request);

    PageResponse<RoomResponse> list(RoomStatus status, Pageable pageable);

    RoomResponse findById(Long id);

    RoomResponse update(Long id, RoomRequest request);

    RoomResponse updateStatus(Long id, RoomStatus status);

    List<RoomResponse> findAvailable(LocalDate checkIn, LocalDate checkOut, int guests);
}
