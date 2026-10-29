package com.hotelaria.models.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permissões granulares verificadas pelo Spring Security.
 */
public enum PermissionName {
    USER_CREATE,
    USER_READ,
    USER_UPDATE,
    USER_STATUS_UPDATE,
    PERMISSION_MANAGE,
    ROOM_CREATE,
    ROOM_UPDATE,
    ROOM_READ,
    AREA_CREATE,
    AREA_UPDATE,
    AREA_READ,
    GUEST_CREATE,
    GUEST_READ,
    GUEST_UPDATE,
    BOOKING_CREATE,
    BOOKING_READ,
    BOOKING_CONFIRM,
    BOOKING_CANCEL,
    CHECKIN,
    CHECKOUT,
    ACCESS_GRANT,
    ACCESS_REVOKE,
    ACCESS_READ,
    ACCESS_REGISTER,
    ACCESS_LOG_READ;

    /**
     * Permissões que o administrador pode conceder individualmente a funcionários.
     * As demais (quartos, áreas, status de usuários e gestão de permissões) são
     * exclusivas do papel de administrador e nunca podem ser concedidas.
     */
    private static final Set<PermissionName> GRANTABLE_TO_EMPLOYEES = EnumSet.of(
            USER_CREATE, USER_READ, USER_UPDATE,
            ACCESS_GRANT, ACCESS_REVOKE, ACCESS_LOG_READ);

    public boolean isGrantableToEmployees() {
        return GRANTABLE_TO_EMPLOYEES.contains(this);
    }
}
