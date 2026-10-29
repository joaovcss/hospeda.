package com.hotelaria.services.support;

import com.hotelaria.models.Booking;
import com.hotelaria.models.BookingAreaAccess;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.enums.AccessDenialReason;
import com.hotelaria.models.enums.AccessGrantStatus;
import com.hotelaria.models.enums.AreaStatus;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.repositories.BookingAreaAccessRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regra central de acesso às áreas. O acesso só é permitido quando:
 * a área está ativa e aberta; existe autorização ativa para uma hospedagem do hóspede;
 * essa hospedagem está em andamento (CHECKED_IN); e o momento está dentro da janela da autorização.
 */
@Component
public class AccessPolicy {

    private final BookingAreaAccessRepository accessRepository;

    public AccessPolicy(BookingAreaAccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    public AccessDecision evaluate(Long guestId, HotelArea area, LocalDateTime moment) {
        if (area.getStatus() != AreaStatus.ACTIVE) {
            return AccessDecision.denied(AccessDenialReason.AREA_UNAVAILABLE, null);
        }

        List<BookingAreaAccess> grants = accessRepository.findByGuestIdAndAreaId(guestId, area.getId());
        if (grants.isEmpty()) {
            return AccessDecision.denied(AccessDenialReason.NOT_AUTHORIZED, null);
        }

        for (BookingAreaAccess grant : grants) {
            if (denialReasonFor(grant, moment) == null) {
                if (!area.isOpenAt(moment.toLocalTime())) {
                    return AccessDecision.denied(AccessDenialReason.OUTSIDE_OPENING_HOURS, grant.getBooking());
                }
                return AccessDecision.granted(grant.getBooking());
            }
        }

        // Nenhuma autorização válida: reporta o motivo da mais relevante
        // (a que cobre o momento atual ou, na falta dela, a da hospedagem mais recente).
        BookingAreaAccess relevant = grants.stream()
                .filter(grant -> grant.isWithinWindow(moment))
                .findFirst()
                .orElse(grants.get(0));
        return AccessDecision.denied(denialReasonFor(relevant, moment), relevant.getBooking());
    }

    private AccessDenialReason denialReasonFor(BookingAreaAccess grant, LocalDateTime moment) {
        Booking booking = grant.getBooking();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return AccessDenialReason.BOOKING_CANCELLED;
        }
        if (grant.getStatus() != AccessGrantStatus.ACTIVE) {
            return AccessDenialReason.AUTHORIZATION_REVOKED;
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            return AccessDenialReason.BOOKING_NOT_ACTIVE;
        }
        if (!grant.isWithinWindow(moment)) {
            return AccessDenialReason.OUTSIDE_AUTHORIZATION_PERIOD;
        }
        return null;
    }
}
