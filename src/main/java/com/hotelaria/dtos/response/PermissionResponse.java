package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.PermissionName;

public record PermissionResponse(
        PermissionName name,
        String description,
        boolean grantableToEmployees
) {
}
