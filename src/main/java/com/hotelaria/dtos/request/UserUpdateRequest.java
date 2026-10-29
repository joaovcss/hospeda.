package com.hotelaria.dtos.request;

import com.hotelaria.models.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 150)
        String email,

        @Schema(description = "Novo tipo (ADMIN ou EMPLOYEE). Somente administradores podem alterar.")
        UserType userType
) {
}
