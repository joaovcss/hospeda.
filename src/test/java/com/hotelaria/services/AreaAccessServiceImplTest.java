package com.hotelaria.services;

import com.hotelaria.dtos.request.AccessValidationRequest;
import com.hotelaria.dtos.request.AreaAccessGrantRequest;
import com.hotelaria.dtos.response.AccessLogResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.exceptions.InvalidStatusTransitionException;
import com.hotelaria.mappers.AccessMapper;
import com.hotelaria.models.AccessLog;
import com.hotelaria.models.Booking;
import com.hotelaria.models.BookingAreaAccess;
import com.hotelaria.models.Guest;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.enums.AccessDenialReason;
import com.hotelaria.models.enums.AccessGrantStatus;
import com.hotelaria.models.enums.AccessResult;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.repositories.AccessLogRepository;
import com.hotelaria.repositories.BookingAreaAccessRepository;
import com.hotelaria.repositories.BookingRepository;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.HotelAreaRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.impl.AccessLogServiceImpl;
import com.hotelaria.services.impl.AreaAccessServiceImpl;
import com.hotelaria.services.support.AccessPolicy;
import com.hotelaria.services.support.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.hotelaria.support.TestFixtures.CLOCK;
import static com.hotelaria.support.TestFixtures.TODAY;
import static com.hotelaria.support.TestFixtures.admin;
import static com.hotelaria.support.TestFixtures.area;
import static com.hotelaria.support.TestFixtures.booking;
import static com.hotelaria.support.TestFixtures.employee;
import static com.hotelaria.support.TestFixtures.grant;
import static com.hotelaria.support.TestFixtures.guest;
import static com.hotelaria.support.TestFixtures.guestActor;
import static com.hotelaria.support.TestFixtures.room;
import static com.hotelaria.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaAccessServiceImplTest {

    @Mock
    private BookingAreaAccessRepository accessRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private HotelAreaRepository areaRepository;
    @Mock
    private GuestRepository guestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccessLogRepository accessLogRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private AreaAccessServiceImpl accessService;
    private AccessLogServiceImpl logService;
    private Guest guest;
    private Booking booking;
    private HotelArea pool;

    @BeforeEach
    void setUp() {
        AccessPolicy policy = new AccessPolicy(accessRepository);
        accessService = new AreaAccessServiceImpl(accessRepository, bookingRepository, areaRepository,
                guestRepository, userRepository, currentUserProvider, new OwnershipGuard(), policy,
                new AccessMapper(), CLOCK);
        logService = new AccessLogServiceImpl(accessLogRepository, guestRepository, areaRepository, userRepository,
                currentUserProvider, new OwnershipGuard(), policy, new AccessMapper(), CLOCK);
        guest = guest(5L);
        booking = booking(10L, guest, room(1L, 2, "300.00"), BookingStatus.CHECKED_IN,
                TODAY.minusDays(1), TODAY.plusDays(2));
        pool = area(7L, null, null);
    }

    @Test
    @DisplayName("Concede acesso com janela padrão igual ao período da hospedagem")
    void grantsWithDefaultWindow() {
        when(currentUserProvider.get()).thenReturn(admin());
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(areaRepository.findById(7L)).thenReturn(Optional.of(pool));
        when(userRepository.getReferenceById(1L)).thenReturn(user(1L, RoleName.ROLE_ADMIN, "a@h.com"));
        when(accessRepository.findByBooking_IdAndArea_Id(10L, 7L)).thenReturn(Optional.empty());
        when(accessRepository.save(any(BookingAreaAccess.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = accessService.grant(10L, new AreaAccessGrantRequest(7L, null, null));

        assertThat(response.validFrom()).isEqualTo(TODAY.minusDays(1).atStartOfDay());
        assertThat(response.validUntil()).isEqualTo(TODAY.plusDays(3).atStartOfDay());
        assertThat(response.status()).isEqualTo(AccessGrantStatus.ACTIVE);
    }

    @Test
    @DisplayName("Autorização fora do período da hospedagem é rejeitada")
    void rejectsWindowOutsideBooking() {
        when(currentUserProvider.get()).thenReturn(admin());
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(areaRepository.findById(7L)).thenReturn(Optional.of(pool));

        AreaAccessGrantRequest request = new AreaAccessGrantRequest(7L,
                TODAY.minusDays(5).atStartOfDay(), TODAY.atStartOfDay());

        assertThatThrownBy(() -> accessService.grant(10L, request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Não concede acesso a hospedagem cancelada")
    void rejectsCancelledBooking() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(currentUserProvider.get()).thenReturn(admin());
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accessService.grant(10L, new AreaAccessGrantRequest(7L, null, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Acesso já ativo para a mesma área gera conflito")
    void rejectsDuplicateActiveGrant() {
        when(currentUserProvider.get()).thenReturn(admin());
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(areaRepository.findById(7L)).thenReturn(Optional.of(pool));
        when(userRepository.getReferenceById(1L)).thenReturn(user(1L, RoleName.ROLE_ADMIN, "a@h.com"));
        when(accessRepository.findByBooking_IdAndArea_Id(10L, 7L)).thenReturn(Optional.of(grant(1L, booking, pool)));

        assertThatThrownBy(() -> accessService.grant(10L, new AreaAccessGrantRequest(7L, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Hóspede não consulta áreas autorizadas de hospedagem de outra pessoa")
    void guestCannotListOthersBookingAreas() {
        when(currentUserProvider.get()).thenReturn(guestActor(6L));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> accessService.listByBooking(10L)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Tentativa negada é registrada com o motivo e o funcionário responsável")
    void deniedAttemptIsLogged() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findWithUserById(5L)).thenReturn(Optional.of(guest));
        when(areaRepository.findById(7L)).thenReturn(Optional.of(pool));
        when(accessRepository.findByGuestIdAndAreaId(5L, 7L)).thenReturn(List.of());
        when(userRepository.getReferenceById(2L)).thenReturn(user(2L, RoleName.ROLE_EMPLOYEE, "f@h.com"));
        when(accessLogRepository.save(any(AccessLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AccessLogResponse response = logService.validateAndRegister(new AccessValidationRequest(5L, 7L));

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AccessResult.DENIED);
        assertThat(captor.getValue().getDenialReason()).isEqualTo(AccessDenialReason.NOT_AUTHORIZED);
        assertThat(captor.getValue().getRegisteredBy().getId()).isEqualTo(2L);
        assertThat(response.result()).isEqualTo(AccessResult.DENIED);
    }

    @Test
    @DisplayName("Tentativa permitida é registrada vinculada à hospedagem")
    void grantedAttemptIsLogged() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findWithUserById(5L)).thenReturn(Optional.of(guest));
        when(areaRepository.findById(7L)).thenReturn(Optional.of(pool));
        when(accessRepository.findByGuestIdAndAreaId(5L, 7L)).thenReturn(List.of(grant(1L, booking, pool)));
        when(userRepository.getReferenceById(2L)).thenReturn(user(2L, RoleName.ROLE_EMPLOYEE, "f@h.com"));
        when(accessLogRepository.save(any(AccessLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AccessLogResponse response = logService.validateAndRegister(new AccessValidationRequest(5L, 7L));

        assertThat(response.result()).isEqualTo(AccessResult.GRANTED);
        assertThat(response.bookingId()).isEqualTo(10L);
        assertThat(response.denialReason()).isNull();
    }
}
