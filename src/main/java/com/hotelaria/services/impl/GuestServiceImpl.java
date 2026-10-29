package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.GuestCreateRequest;
import com.hotelaria.dtos.request.GuestUpdateRequest;
import com.hotelaria.dtos.response.GuestResponse;
import com.hotelaria.dtos.response.GuestSummaryResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.GuestMapper;
import com.hotelaria.models.Guest;
import com.hotelaria.models.Role;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.RoleRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.AuthenticatedUser;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.GuestService;
import com.hotelaria.services.support.OwnershipGuard;
import com.hotelaria.utils.CpfUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final OwnershipGuard ownershipGuard;
    private final GuestMapper guestMapper;

    public GuestServiceImpl(GuestRepository guestRepository, UserRepository userRepository,
                            RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                            CurrentUserProvider currentUserProvider, OwnershipGuard ownershipGuard,
                            GuestMapper guestMapper) {
        this.guestRepository = guestRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.ownershipGuard = ownershipGuard;
        this.guestMapper = guestMapper;
    }

    /**
     * Cria, na mesma transação, as credenciais (User com ROLE_GUEST) e o cadastro do hóspede.
     * CPF e e-mail únicos impedem contas duplicadas para a mesma pessoa.
     */
    @Override
    @Transactional
    public GuestResponse create(GuestCreateRequest request) {
        String cpf = CpfUtils.normalize(request.cpf());
        String email = normalizeEmail(request.email());
        if (guestRepository.existsByCpf(cpf)) {
            throw new DuplicateResourceException("Já existe um hóspede cadastrado com este CPF");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Este e-mail já está em uso");
        }

        Role guestRole = roleRepository.findByName(RoleName.ROLE_GUEST)
                .orElseThrow(() -> new IllegalStateException("Papel ROLE_GUEST não cadastrado"));
        User user = userRepository.save(new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), guestRole));
        Guest guest = guestRepository.save(new Guest(user, cpf, request.phone().trim(), request.birthDate(),
                guestMapper.toAddress(request.address())));
        return guestMapper.toResponse(guest);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GuestSummaryResponse> list(String search, Pageable pageable) {
        Page<Guest> page = StringUtils.hasText(search)
                ? guestRepository.search(search.trim(), CpfUtils.normalize(search), pageable)
                : guestRepository.findAllWithUser(pageable);
        return PageResponse.from(page.map(guestMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public GuestResponse findById(Long id) {
        ownershipGuard.ensureCanAccessGuest(currentUserProvider.get(), id);
        return guestMapper.toResponse(findGuest(id));
    }

    @Override
    @Transactional(readOnly = true)
    public GuestResponse getMyProfile() {
        Long guestId = ownershipGuard.requireGuestId(currentUserProvider.get());
        return guestMapper.toResponse(findGuest(guestId));
    }

    @Override
    @Transactional
    public GuestResponse update(Long id, GuestUpdateRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        ownershipGuard.ensureCanAccessGuest(actor, id);
        Guest guest = findGuest(id);
        User user = guest.getUser();

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new DuplicateResourceException("Este e-mail já está em uso");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        guest.setPhone(request.phone().trim());
        guest.setBirthDate(request.birthDate());
        guest.setAddress(guestMapper.toAddress(request.address()));
        return guestMapper.toResponse(guest);
    }

    private Guest findGuest(Long id) {
        return guestRepository.findWithUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hóspede", id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
