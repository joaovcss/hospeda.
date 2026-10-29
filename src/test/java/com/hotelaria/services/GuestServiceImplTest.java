package com.hotelaria.services;

import com.hotelaria.dtos.request.GuestCreateRequest;
import com.hotelaria.dtos.response.GuestResponse;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.mappers.GuestMapper;
import com.hotelaria.models.Guest;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.RoleRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.impl.GuestServiceImpl;
import com.hotelaria.services.support.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.hotelaria.support.TestFixtures.guest;
import static com.hotelaria.support.TestFixtures.guestActor;
import static com.hotelaria.support.TestFixtures.role;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestServiceImplTest {

    @Mock
    private GuestRepository guestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private GuestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuestServiceImpl(guestRepository, userRepository, roleRepository, passwordEncoder,
                currentUserProvider, new OwnershipGuard(), new GuestMapper());
    }

    private GuestCreateRequest request() {
        return new GuestCreateRequest("João Souza", "Joao@Mail.com", "052.982.247-25", "11 98888-7777",
                null, null, "SenhaHospede1");
    }

    @Test
    @DisplayName("Cadastro de hóspede cria credenciais com papel ROLE_GUEST e CPF somente com dígitos")
    void createsGuestWithCredentials() {
        when(guestRepository.existsByCpf("05298224725")).thenReturn(false);
        when(userRepository.existsByEmail("joao@mail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_GUEST)).thenReturn(Optional.of(role(RoleName.ROLE_GUEST)));
        when(passwordEncoder.encode("SenhaHospede1")).thenReturn("$2a$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestRepository.save(any(Guest.class))).thenAnswer(inv -> inv.getArgument(0));

        GuestResponse response = service.create(request());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoleName()).isEqualTo(RoleName.ROLE_GUEST);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(response.cpf()).isEqualTo("05298224725");
        assertThat(response.email()).isEqualTo("joao@mail.com");
    }

    @Test
    @DisplayName("CPF duplicado impede conta duplicada")
    void rejectsDuplicateCpf() {
        when(guestRepository.existsByCpf("05298224725")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("E-mail já usado por outro usuário é rejeitado")
    void rejectsDuplicateEmail() {
        when(guestRepository.existsByCpf("05298224725")).thenReturn(false);
        when(userRepository.existsByEmail("joao@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hóspede não pode consultar dados de outro hóspede")
    void guestCannotReadOtherGuest() {
        when(currentUserProvider.get()).thenReturn(guestActor(5L));

        assertThatThrownBy(() -> service.findById(6L)).isInstanceOf(ForbiddenOperationException.class);
        verify(guestRepository, never()).findWithUserById(any());
    }

    @Test
    @DisplayName("Hóspede consulta o próprio cadastro")
    void guestReadsOwnProfile() {
        when(currentUserProvider.get()).thenReturn(guestActor(5L));
        when(guestRepository.findWithUserById(5L)).thenReturn(Optional.of(guest(5L)));

        assertThat(service.getMyProfile().id()).isEqualTo(5L);
    }
}
