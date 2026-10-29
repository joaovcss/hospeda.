package com.hotelaria.mappers;

import com.hotelaria.dtos.response.AccessLogResponse;
import com.hotelaria.dtos.response.BookingAreaAccessResponse;
import com.hotelaria.models.AccessLog;
import com.hotelaria.models.BookingAreaAccess;
import com.hotelaria.models.User;
import org.springframework.stereotype.Component;

@Component
public class AccessMapper {

    public BookingAreaAccessResponse toResponse(BookingAreaAccess access) {
        return new BookingAreaAccessResponse(
                access.getId(),
                access.getBooking().getId(),
                access.getArea().getId(),
                access.getArea().getName(),
                access.getArea().getLocation(),
                access.getValidFrom(),
                access.getValidUntil(),
                access.getStatus(),
                access.getCreatedAt(),
                access.getRevokedAt());
    }

    public AccessLogResponse toResponse(AccessLog log) {
        User registeredBy = log.getRegisteredBy();
        return new AccessLogResponse(
                log.getId(),
                log.getGuest().getId(),
                log.getGuest().getUser().getFullName(),
                log.getBooking() == null ? null : log.getBooking().getId(),
                log.getArea().getId(),
                log.getArea().getName(),
                log.getAttemptedAt(),
                log.getResult(),
                log.getDenialReason(),
                log.getDenialReason() == null ? null : log.getDenialReason().getDescription(),
                registeredBy == null ? null : registeredBy.getId(),
                registeredBy == null ? null : registeredBy.getFullName());
    }
}
