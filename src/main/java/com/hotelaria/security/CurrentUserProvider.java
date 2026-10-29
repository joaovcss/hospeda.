package com.hotelaria.security;

import com.hotelaria.exceptions.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Fornece o usuário autenticado aos services. A identidade vem sempre do token
 * validado, nunca de IDs enviados pelo cliente.
 */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new InvalidCredentialsException("Autenticação necessária");
    }
}
