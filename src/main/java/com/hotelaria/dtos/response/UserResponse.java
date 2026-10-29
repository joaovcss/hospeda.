package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.UserType;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        UserType userType,
        boolean active,
        Set<PermissionName> extraPermissions,
        Set<PermissionName> effectivePermissions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
