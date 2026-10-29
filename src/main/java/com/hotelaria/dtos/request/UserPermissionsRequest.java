package com.hotelaria.dtos.request;

import com.hotelaria.models.enums.PermissionName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserPermissionsRequest(
        @Schema(description = "Conjunto completo de permissões extras do funcionário (substitui o atual)",
                example = "[\"USER_CREATE\"]")
        @NotNull
        Set<PermissionName> permissions
) {
}
