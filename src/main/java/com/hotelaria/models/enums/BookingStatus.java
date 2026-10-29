package com.hotelaria.models.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Status de uma hospedagem e as transições permitidas entre eles.
 * CHECKED_OUT e CANCELLED são estados finais: a hospedagem não pode ser reaberta.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    /** Status que ocupam o quarto no período e, portanto, geram conflito. */
    public static final Set<BookingStatus> BLOCKING = EnumSet.of(CONFIRMED, CHECKED_IN);

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(CHECKED_IN, CANCELLED),
            CHECKED_IN, EnumSet.of(CHECKED_OUT),
            CHECKED_OUT, EnumSet.noneOf(BookingStatus.class),
            CANCELLED, EnumSet.noneOf(BookingStatus.class));

    public boolean canTransitionTo(BookingStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
