package com.hotelaria.services.support;

import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.models.Booking;
import com.hotelaria.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

/**
 * Garante que hóspedes só acessem os próprios dados. Funcionários e administradores
 * passam por aqui sem restrição: suas permissões já foram verificadas no controller.
 */
@Component
public class OwnershipGuard {

    public void ensureCanAccessBooking(AuthenticatedUser actor, Booking booking) {
        if (actor.isGuest() && !booking.belongsToGuest(actor.getGuestId())) {
            throw new ForbiddenOperationException("Você não tem acesso a esta hospedagem");
        }
    }

    public void ensureCanAccessGuest(AuthenticatedUser actor, Long guestId) {
        if (actor.isGuest() && !guestId.equals(actor.getGuestId())) {
            throw new ForbiddenOperationException("Hóspedes só podem consultar os próprios dados");
        }
    }

    public Long requireGuestId(AuthenticatedUser actor) {
        if (!actor.isGuest() || actor.getGuestId() == null) {
            throw new ForbiddenOperationException("Operação disponível apenas para hóspedes");
        }
        return actor.getGuestId();
    }
}
