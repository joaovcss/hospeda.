package com.hotelaria.repositories;

import com.hotelaria.models.User;
import com.hotelaria.models.enums.RoleName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /** Carrega o usuário com papel e permissões em uma única consulta. */
    @EntityGraph(attributePaths = {"role", "role.permissions", "extraPermissions"})
    Optional<User> findWithAuthoritiesById(Long id);

    @EntityGraph(attributePaths = {"role", "role.permissions", "extraPermissions"})
    Optional<User> findWithAuthoritiesByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByRole_Name(RoleName roleName);
}
