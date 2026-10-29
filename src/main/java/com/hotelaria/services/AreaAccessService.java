package com.hotelaria.services;

import com.hotelaria.dtos.request.AreaAccessGrantRequest;
import com.hotelaria.dtos.response.AccessCheckResponse;
import com.hotelaria.dtos.response.BookingAreaAccessResponse;

import java.util.List;

/**
 * Concessão, revogação e consulta das autorizações de acesso às áreas do hotel.
 */
public interface AreaAccessService {

    BookingAreaAccessResponse grant(Long bookingId, AreaAccessGrantRequest request);

    void revoke(Long bookingId, Long areaId);

    List<BookingAreaAccessResponse> listByBooking(Long bookingId);

    List<BookingAreaAccessResponse> listByGuest(Long guestId);

    List<BookingAreaAccessResponse> listMine();

    /** Verifica a autorização no momento atual, sem registrar tentativa de acesso. */
    AccessCheckResponse check(Long guestId, Long areaId);
}
