package com.hotelaria.mappers;

import com.hotelaria.dtos.response.AuthenticatedUserResponse;
import com.hotelaria.dtos.response.PermissionResponse;
import com.hotelaria.dtos.response.UserResponse;
import com.hotelaria.models.Permission;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.UserType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                UserType.fromRoleName(user.getRoleName()),
                user.isActive(),
                extraPermissionNames(user),
                user.getEffectivePermissions(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public AuthenticatedUserResponse toAuthenticatedResponse(User user, Long guestId) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                UserType.fromRoleName(user.getRoleName()),
                guestId,
                user.getEffectivePermissions());
    }

    public PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getName(),
                permission.getDescription(),
                permission.getName().isGrantableToEmployees());
    }

    private Set<PermissionName> extraPermissionNames(User user) {
        Set<PermissionName> names = EnumSet.noneOf(PermissionName.class);
        user.getExtraPermissions().forEach(p -> names.add(p.getName()));
        return names;
    }
}
