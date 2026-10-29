package com.hotelaria.dtos.request;

import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserCreateRequest(
        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 150)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @Schema(description = "ADMIN ou EMPLOYEE. Hóspedes são cadastrados em /api/guests.")
        @NotNull
        UserType userType,

        @Schema(description = "Permissões extras (somente administradores podem conceder)")
        Set<PermissionName> permissions
) {
}
