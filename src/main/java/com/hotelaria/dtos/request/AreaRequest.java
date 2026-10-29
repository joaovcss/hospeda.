package com.hotelaria.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record AreaRequest(
        @Schema(example = "Piscina")
        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @Schema(example = "Térreo - Bloco B")
        @NotBlank @Size(max = 150)
        String location,

        @Schema(description = "Opcional; informe junto com closingTime", type = "string", example = "08:00")
        LocalTime openingTime,

        @Schema(description = "Opcional; pode ser menor que openingTime para horários após a meia-noite",
                type = "string", example = "22:00")
        LocalTime closingTime
) {
}
