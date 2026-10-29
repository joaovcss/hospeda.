package com.hotelaria.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookingCreateRequest(
        @Schema(description = "Obrigatório para funcionários/administradores; ignorado para hóspedes "
                + "(a reserva é sempre do hóspede autenticado)")
        Long guestId,

        @NotNull
        Long roomId,

        @Schema(example = "2026-12-20")
        @NotNull
        LocalDate checkInDate,

        @Schema(example = "2026-12-23")
        @NotNull
        LocalDate checkOutDate,

        @NotNull @Min(1)
        Integer numberOfGuests,

        @Size(max = 500)
        String notes
) {
}
