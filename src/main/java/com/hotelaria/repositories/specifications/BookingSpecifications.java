package com.hotelaria.repositories.specifications;

import com.hotelaria.models.Booking;
import com.hotelaria.models.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Filtros opcionais para consultas de hospedagens. Parâmetros nulos são ignorados.
 */
public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> hasGuest(Long guestId) {
        return (root, query, cb) -> guestId == null ? null : cb.equal(root.get("guest").get("id"), guestId);
    }

    public static Specification<Booking> hasRoom(Long roomId) {
        return (root, query, cb) -> roomId == null ? null : cb.equal(root.get("room").get("id"), roomId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Hospedagens cujo período se sobrepõe ao intervalo [from, to]. */
    public static Specification<Booking> overlapsPeriod(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("checkInDate"), to);
            }
            if (to == null) {
                return cb.greaterThan(root.get("checkOutDate"), from);
            }
            return cb.and(
                    cb.lessThanOrEqualTo(root.get("checkInDate"), to),
                    cb.greaterThan(root.get("checkOutDate"), from));
        };
    }

    /**
     * Hospedagens atuais: em andamento (CHECKED_IN) ou pendentes/confirmadas
     * cuja data de saída ainda não passou.
     */
    public static Specification<Booking> isCurrent(LocalDate today) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("status"), BookingStatus.CHECKED_IN),
                cb.and(
                        root.get("status").in(BookingStatus.PENDING, BookingStatus.CONFIRMED),
                        cb.greaterThanOrEqualTo(root.get("checkOutDate"), today)));
    }

    /** Hospedagens anteriores: tudo que não é atual (finalizadas, canceladas ou expiradas). */
    public static Specification<Booking> isPast(LocalDate today) {
        return Specification.not(isCurrent(today));
    }
}
