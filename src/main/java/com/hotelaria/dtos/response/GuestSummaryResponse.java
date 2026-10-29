package com.hotelaria.dtos.response;

/**
 * Representação reduzida usada em listagens: o CPF é mascarado.
 */
public record GuestSummaryResponse(
        Long id,
        String fullName,
        String email,
        String maskedCpf
) {
}
