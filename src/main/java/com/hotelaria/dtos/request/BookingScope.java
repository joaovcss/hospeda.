package com.hotelaria.dtos.request;

/**
 * Filtro usado pelo hóspede para consultar as próprias hospedagens.
 */
public enum BookingScope {
    /** Pendentes, confirmadas ou em andamento que ainda não terminaram. */
    CURRENT,
    /** Finalizadas, canceladas ou com data de saída já passada. */
    PAST,
    ALL
}
