package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.UserType;

import java.util.Set;

public record AuthenticatedUserResponse(
        Long id,
        String fullName,
        String email,
        UserType userType,
        /* Preenchido apenas para hóspedes. */
        Long guestId,
        Set<PermissionName> permissions
) {
}
