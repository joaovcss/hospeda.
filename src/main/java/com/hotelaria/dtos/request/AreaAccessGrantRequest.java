package com.hotelaria.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AreaAccessGrantRequest(
        @NotNull
        Long areaId,

        @Schema(description = "Início da autorização. Padrão: 00:00 do dia de entrada da hospedagem.")
        LocalDateTime validFrom,

        @Schema(description = "Fim (exclusivo) da autorização. Padrão: 00:00 do dia seguinte à saída.")
        LocalDateTime validUntil
) {
}
