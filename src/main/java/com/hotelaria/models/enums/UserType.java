package com.hotelaria.models.enums;

/**
 * Tipo de usuário exposto pela API. Corresponde um-para-um a um {@link RoleName}.
 */
public enum UserType {
    ADMIN(RoleName.ROLE_ADMIN),
    EMPLOYEE(RoleName.ROLE_EMPLOYEE),
    GUEST(RoleName.ROLE_GUEST);

    private final RoleName roleName;

    UserType(RoleName roleName) {
        this.roleName = roleName;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public static UserType fromRoleName(RoleName roleName) {
        for (UserType type : values()) {
            if (type.roleName == roleName) {
                return type;
            }
        }
        throw new IllegalArgumentException("Papel desconhecido: " + roleName);
    }
}
