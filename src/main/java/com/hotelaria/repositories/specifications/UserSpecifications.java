package com.hotelaria.repositories.specifications;

import com.hotelaria.models.User;
import com.hotelaria.models.enums.RoleName;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros opcionais para a listagem de usuários. Parâmetros nulos são ignorados.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRole(RoleName roleName) {
        return (root, query, cb) -> roleName == null ? null : cb.equal(root.get("role").get("name"), roleName);
    }

    public static Specification<User> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
