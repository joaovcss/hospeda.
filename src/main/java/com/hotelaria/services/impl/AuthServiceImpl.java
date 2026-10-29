package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.ChangePasswordRequest;
import com.hotelaria.dtos.request.LoginRequest;
import com.hotelaria.dtos.response.AuthenticatedUserResponse;
import com.hotelaria.dtos.response.LoginResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.InvalidCredentialsException;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.UserMapper;
import com.hotelaria.models.Guest;
import com.hotelaria.models.User;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.AuthenticatedUser;
import com.hotelaria.security.AuthenticatedUserLoader;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.security.JwtService;
import com.hotelaria.services.AuthService;
import com.hotelaria.utils.CpfUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticatedUserLoader userLoader;
    private final CurrentUserProvider currentUserProvider;
    private final UserMapper userMapper;

    /**
     * Hash usado quando o login não existe, para que a resposta leve o mesmo tempo
     * de uma senha incorreta e não revele quais e-mails/CPFs estão cadastrados.
     */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository, GuestRepository guestRepository,
                           PasswordEncoder passwordEncoder, JwtService jwtService,
                           AuthenticatedUserLoader userLoader, CurrentUserProvider currentUserProvider,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.guestRepository = guestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userLoader = userLoader;
        this.currentUserProvider = currentUserProvider;
        this.userMapper = userMapper;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = findByLogin(request.login().trim());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!passwordMatches || !user.isActive()) {
            throw new InvalidCredentialsException();
        }

        AuthenticatedUser principal = userLoader.toPrincipal(user);
        String token = jwtService.generateToken(principal);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(),
                userMapper.toAuthenticatedResponse(user, principal.getGuestId()));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getAuthenticatedUser() {
        AuthenticatedUser actor = currentUserProvider.get();
        User user = userRepository.findWithAuthoritiesById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", actor.getId()));
        return userMapper.toAuthenticatedResponse(user, actor.getGuestId());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        User user = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", actor.getId()));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("A senha atual está incorreta");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessRuleException("A nova senha deve ser diferente da senha atual");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    /** Hóspedes podem entrar com CPF; qualquer usuário pode entrar com e-mail. */
    private Optional<User> findByLogin(String login) {
        if (CpfUtils.looksLikeCpf(login)) {
            return guestRepository.findByCpf(CpfUtils.normalize(login)).map(Guest::getUser);
        }
        return userRepository.findWithAuthoritiesByEmail(login.toLowerCase(Locale.ROOT));
    }
}
