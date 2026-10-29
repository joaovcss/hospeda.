package com.hotelaria.security;

import com.hotelaria.models.User;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Carrega o usuário a partir do banco a cada requisição autenticada. Assim, desativações
 * e alterações de permissões passam a valer imediatamente, mesmo com tokens já emitidos.
 */
@Component
public class AuthenticatedUserLoader {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;

    public AuthenticatedUserLoader(UserRepository userRepository, GuestRepository guestRepository) {
        this.userRepository = userRepository;
        this.guestRepository = guestRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> loadActiveById(Long userId) {
        return userRepository.findWithAuthoritiesById(userId)
                .filter(User::isActive)
                .map(this::toPrincipal);
    }

    public AuthenticatedUser toPrincipal(User user) {
        Long guestId = user.hasRole(RoleName.ROLE_GUEST)
                ? guestRepository.findIdByUserId(user.getId()).orElse(null)
                : null;
        return AuthenticatedUser.from(user, guestId);
    }
}
