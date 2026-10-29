package com.hotelaria.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "E-mail (qualquer usuário) ou CPF (hóspedes)", example = "admin@hotel.com")
        @NotBlank @Size(max = 150)
        String login,

        @Schema(example = "SenhaForte#2026")
        @NotBlank @Size(max = 72)
        String password
) {
}
