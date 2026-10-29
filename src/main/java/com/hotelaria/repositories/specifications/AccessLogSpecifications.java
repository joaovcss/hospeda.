package com.hotelaria.repositories.specifications;

import com.hotelaria.models.AccessLog;
import com.hotelaria.models.enums.AccessResult;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Filtros opcionais para o histórico de acessos. Parâmetros nulos são ignorados.
 */
public final class AccessLogSpecifications {

    private AccessLogSpecifications() {
    }

    public static Specification<AccessLog> hasGuest(Long guestId) {
        return (root, query, cb) -> guestId == null ? null : cb.equal(root.get("guest").get("id"), guestId);
    }

    public static Specification<AccessLog> hasArea(Long areaId) {
        return (root, query, cb) -> areaId == null ? null : cb.equal(root.get("area").get("id"), areaId);
    }

    public static Specification<AccessLog> hasBooking(Long bookingId) {
        return (root, query, cb) -> bookingId == null ? null : cb.equal(root.get("booking").get("id"), bookingId);
    }

    public static Specification<AccessLog> hasResult(AccessResult result) {
        return (root, query, cb) -> result == null ? null : cb.equal(root.get("result"), result);
    }

    public static Specification<AccessLog> attemptedFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("attemptedAt"), from);
    }

    public static Specification<AccessLog> attemptedUntil(LocalDateTime until) {
        return (root, query, cb) -> until == null ? null : cb.lessThan(root.get("attemptedAt"), until);
    }
}
