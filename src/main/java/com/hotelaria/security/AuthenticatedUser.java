package com.hotelaria.security;

import com.hotelaria.models.User;
import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Principal mantido no SecurityContext durante a requisição. As authorities combinam
 * o papel (ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_GUEST) e as permissões efetivas do usuário.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final RoleName role;
    private final Long guestId;
    private final Set<PermissionName> permissions;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(Long id, String email, String fullName, RoleName role, Long guestId,
                             Set<PermissionName> permissions) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.guestId = guestId;
        this.permissions = permissions.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(PermissionName.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(permissions));
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new SimpleGrantedAuthority(role.name()));
        this.permissions.forEach(p -> list.add(new SimpleGrantedAuthority(p.name())));
        this.authorities = Collections.unmodifiableList(list);
    }

    public static AuthenticatedUser from(User user, Long guestId) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRoleName(),
                guestId, user.getEffectivePermissions());
    }

    public boolean isAdmin() {
        return role == RoleName.ROLE_ADMIN;
    }

    public boolean isGuest() {
        return role == RoleName.ROLE_GUEST;
    }

    public boolean hasPermission(PermissionName permission) {
        return permissions.contains(permission);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public RoleName getRole() {
        return role;
    }

    /** Identificador do cadastro de hóspede; nulo para administradores e funcionários. */
    public Long getGuestId() {
        return guestId;
    }

    public Set<PermissionName> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** A senha nunca é mantida no principal. */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
