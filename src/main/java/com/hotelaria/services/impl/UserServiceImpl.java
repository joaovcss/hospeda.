package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.UserCreateRequest;
import com.hotelaria.dtos.request.UserUpdateRequest;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.PermissionResponse;
import com.hotelaria.dtos.response.UserResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.UserMapper;
import com.hotelaria.models.Permission;
import com.hotelaria.models.Role;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.models.enums.UserType;
import com.hotelaria.repositories.PermissionRepository;
import com.hotelaria.repositories.RoleRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.repositories.specifications.UserSpecifications;
import com.hotelaria.security.AuthenticatedUser;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PermissionRepository permissionRepository, PasswordEncoder passwordEncoder,
                           CurrentUserProvider currentUserProvider, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        Set<PermissionName> permissions = request.permissions() == null ? Set.of() : request.permissions();

        if (request.userType() == UserType.GUEST) {
            throw new BusinessRuleException("Hóspedes devem ser cadastrados pelo endpoint /api/guests");
        }
        if (!actor.isAdmin()) {
            if (request.userType() == UserType.ADMIN) {
                throw new ForbiddenOperationException("Somente administradores podem cadastrar administradores");
            }
            if (!permissions.isEmpty()) {
                throw new ForbiddenOperationException("Somente administradores podem conceder permissões");
            }
        }
        if (request.userType() == UserType.ADMIN && !permissions.isEmpty()) {
            throw new BusinessRuleException("Administradores já possuem todas as permissões");
        }
        validateGrantable(permissions);

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Já existe um usuário com este e-mail");
        }

        User user = new User(request.fullName().trim(), email, passwordEncoder.encode(request.password()),
                findRole(request.userType().getRoleName()));
        user.getExtraPermissions().addAll(loadPermissions(permissions));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(UserType userType, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.hasRole(userType == null ? null : userType.getRoleName()),
                UserSpecifications.isActive(active));
        return PageResponse.from(userRepository.findAll(spec, pageable).map(userMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        User user = findUser(id);

        if (user.hasRole(RoleName.ROLE_GUEST)) {
            throw new BusinessRuleException("Dados de hóspedes devem ser atualizados pelo endpoint /api/guests");
        }
        UserType currentType = UserType.fromRoleName(user.getRoleName());
        boolean typeChange = request.userType() != null && request.userType() != currentType;
        if (!actor.isAdmin()) {
            if (user.hasRole(RoleName.ROLE_ADMIN)) {
                throw new ForbiddenOperationException("Somente administradores podem alterar dados de administradores");
            }
            if (typeChange) {
                throw new ForbiddenOperationException("Somente administradores podem alterar o tipo de usuário");
            }
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Já existe um usuário com este e-mail");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);

        if (typeChange) {
            changeType(user, request.userType(), actor);
        }
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, boolean active) {
        AuthenticatedUser actor = currentUserProvider.get();
        if (!active && actor.getId().equals(id)) {
            throw new BusinessRuleException("Você não pode desativar a sua própria conta");
        }
        User user = findUser(id);
        user.setActive(active);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updatePermissions(Long id, Set<PermissionName> permissions) {
        User user = findUser(id);
        if (!user.hasRole(RoleName.ROLE_EMPLOYEE)) {
            throw new BusinessRuleException("Permissões extras só podem ser concedidas a funcionários");
        }
        validateGrantable(permissions);
        user.getExtraPermissions().clear();
        user.getExtraPermissions().addAll(loadPermissions(permissions));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll(Sort.by("id")).stream()
                .map(userMapper::toPermissionResponse)
                .toList();
    }

    private void changeType(User user, UserType newType, AuthenticatedUser actor) {
        if (newType == UserType.GUEST) {
            throw new BusinessRuleException("Não é possível converter um usuário em hóspede");
        }
        if (user.getId().equals(actor.getId())) {
            throw new BusinessRuleException("Você não pode alterar o seu próprio tipo de usuário");
        }
        user.setRole(findRole(newType.getRoleName()));
        if (newType == UserType.ADMIN) {
            user.getExtraPermissions().clear();
        }
    }

    private void validateGrantable(Set<PermissionName> permissions) {
        Set<PermissionName> forbidden = permissions.stream()
                .filter(p -> !p.isGrantableToEmployees())
                .collect(Collectors.toSet());
        if (!forbidden.isEmpty()) {
            throw new BusinessRuleException("As permissões " + forbidden
                    + " são exclusivas do administrador e não podem ser concedidas a funcionários");
        }
    }

    private List<Permission> loadPermissions(Set<PermissionName> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        List<Permission> permissions = permissionRepository.findByNameIn(names);
        if (permissions.size() != names.size()) {
            throw new IllegalStateException("Permissões não cadastradas no banco: verifique as migrations");
        }
        return permissions;
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Papel não cadastrado: " + roleName));
    }

    private User findUser(Long id) {
        return userRepository.findWithAuthoritiesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
