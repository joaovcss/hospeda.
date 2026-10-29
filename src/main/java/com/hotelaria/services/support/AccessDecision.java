package com.hotelaria.services.support;

import com.hotelaria.models.Booking;
import com.hotelaria.models.enums.AccessDenialReason;

/**
 * Resultado da avaliação de acesso de um hóspede a uma área.
 *
 * @param booking hospedagem considerada na decisão (pode ser nula quando não há autorização)
 * @param denialReason motivo da negativa; nulo quando o acesso é permitido
 */
public record AccessDecision(boolean allowed, Booking booking, AccessDenialReason denialReason) {

    public static AccessDecision granted(Booking booking) {
        return new AccessDecision(true, booking, null);
    }

    public static AccessDecision denied(AccessDenialReason reason, Booking booking) {
        return new AccessDecision(false, booking, reason);
    }
}
