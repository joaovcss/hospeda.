package com.hotelaria.services;

import com.hotelaria.dtos.request.UserCreateRequest;
import com.hotelaria.dtos.request.UserUpdateRequest;
import com.hotelaria.dtos.response.UserResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.mappers.UserMapper;
import com.hotelaria.models.Permission;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.models.enums.UserType;
import com.hotelaria.repositories.PermissionRepository;
import com.hotelaria.repositories.RoleRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.hotelaria.support.TestFixtures.admin;
import static com.hotelaria.support.TestFixtures.employee;
import static com.hotelaria.support.TestFixtures.role;
import static com.hotelaria.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, roleRepository, permissionRepository, passwordEncoder,
                currentUserProvider, new UserMapper());
    }

    private UserCreateRequest createRequest(UserType type, Set<PermissionName> permissions) {
        return new UserCreateRequest("Maria Silva", " Maria@Hotel.com ", "SenhaForte123", type, permissions);
    }

    @Test
    @DisplayName("Funcionário com USER_CREATE cadastra funcionário com senha criptografada e e-mail normalizado")
    void employeeWithPermissionCreatesEmployee() {
        when(currentUserProvider.get()).thenReturn(employee(PermissionName.USER_CREATE));
        when(userRepository.existsByEmail("maria@hotel.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(role(RoleName.ROLE_EMPLOYEE)));
        when(passwordEncoder.encode("SenhaForte123")).thenReturn("$2a$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.create(createRequest(UserType.EMPLOYEE, null));

        assertThat(response.email()).isEqualTo("maria@hotel.com");
        assertThat(response.userType()).isEqualTo(UserType.EMPLOYEE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Funcionário não pode cadastrar administradores")
    void employeeCannotCreateAdmin() {
        when(currentUserProvider.get()).thenReturn(employee(PermissionName.USER_CREATE));

        assertThatThrownBy(() -> service.create(createRequest(UserType.ADMIN, null)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Funcionário não pode conceder permissões ao cadastrar usuários")
    void employeeCannotGrantPermissions() {
        when(currentUserProvider.get()).thenReturn(employee(PermissionName.USER_CREATE));

        assertThatThrownBy(() -> service.create(createRequest(UserType.EMPLOYEE, Set.of(PermissionName.USER_CREATE))))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hóspedes não são criados por /api/users")
    void guestTypeIsRejected() {
        when(currentUserProvider.get()).thenReturn(admin());

        assertThatThrownBy(() -> service.create(createRequest(UserType.GUEST, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("E-mail duplicado retorna conflito")
    void duplicateEmailIsRejected() {
        when(currentUserProvider.get()).thenReturn(admin());
        when(userRepository.existsByEmail("maria@hotel.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest(UserType.EMPLOYEE, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Administrador cadastra funcionário já com USER_CREATE")
    void adminCreatesEmployeeWithUserCreate() {
        when(currentUserProvider.get()).thenReturn(admin());
        when(userRepository.existsByEmail("maria@hotel.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(role(RoleName.ROLE_EMPLOYEE)));
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(permissionRepository.findByNameIn(Set.of(PermissionName.USER_CREATE)))
                .thenReturn(List.of(new Permission(PermissionName.USER_CREATE, "Cadastrar usuários")));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.create(createRequest(UserType.EMPLOYEE, Set.of(PermissionName.USER_CREATE)));

        assertThat(response.extraPermissions()).containsExactly(PermissionName.USER_CREATE);
        assertThat(response.effectivePermissions()).contains(PermissionName.USER_CREATE);
    }

    @Test
    @DisplayName("Permissões administrativas não podem ser concedidas a funcionários")
    void adminOnlyPermissionsAreNotGrantable() {
        when(userRepository.findWithAuthoritiesById(3L))
                .thenReturn(Optional.of(user(3L, RoleName.ROLE_EMPLOYEE, "f@hotel.com")));

        assertThatThrownBy(() -> service.updatePermissions(3L, Set.of(PermissionName.ROOM_CREATE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ROOM_CREATE");
    }

    @Test
    @DisplayName("Permissões extras só podem ser concedidas a funcionários")
    void permissionsOnlyForEmployees() {
        when(userRepository.findWithAuthoritiesById(3L))
                .thenReturn(Optional.of(user(3L, RoleName.ROLE_GUEST, "g@mail.com")));

        assertThatThrownBy(() -> service.updatePermissions(3L, Set.of(PermissionName.USER_CREATE)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Administrador concede e revoga USER_CREATE de um funcionário")
    void grantAndRevokeUserCreate() {
        User employeeUser = user(3L, RoleName.ROLE_EMPLOYEE, "f@hotel.com");
        when(userRepository.findWithAuthoritiesById(3L)).thenReturn(Optional.of(employeeUser));
        when(permissionRepository.findByNameIn(Set.of(PermissionName.USER_CREATE)))
                .thenReturn(List.of(new Permission(PermissionName.USER_CREATE, "Cadastrar usuários")));

        assertThat(service.updatePermissions(3L, Set.of(PermissionName.USER_CREATE)).effectivePermissions())
                .contains(PermissionName.USER_CREATE);
        assertThat(service.updatePermissions(3L, Set.of()).effectivePermissions())
                .doesNotContain(PermissionName.USER_CREATE);
    }

    @Test
    @DisplayName("Administrador não pode desativar a própria conta")
    void adminCannotDeactivateSelf() {
        when(currentUserProvider.get()).thenReturn(admin());

        assertThatThrownBy(() -> service.updateStatus(1L, false)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Funcionário com USER_UPDATE não altera administradores nem tipo de usuário")
    void employeeCannotUpdateAdminOrType() {
        when(currentUserProvider.get()).thenReturn(employee(PermissionName.USER_UPDATE));
        when(userRepository.findWithAuthoritiesById(1L))
                .thenReturn(Optional.of(user(1L, RoleName.ROLE_ADMIN, "admin@hotel.com")));
        when(userRepository.findWithAuthoritiesById(3L))
                .thenReturn(Optional.of(user(3L, RoleName.ROLE_EMPLOYEE, "f@hotel.com")));

        assertThatThrownBy(() -> service.update(1L, new UserUpdateRequest("X", "admin@hotel.com", null)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.update(3L, new UserUpdateRequest("X", "f@hotel.com", UserType.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
