package com.hotelaria.services;

import com.hotelaria.models.Booking;
import com.hotelaria.models.BookingAreaAccess;
import com.hotelaria.models.Guest;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.Room;
import com.hotelaria.models.enums.AccessDenialReason;
import com.hotelaria.models.enums.AreaStatus;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.repositories.BookingAreaAccessRepository;
import com.hotelaria.services.support.AccessDecision;
import com.hotelaria.services.support.AccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.hotelaria.support.TestFixtures.TODAY;
import static com.hotelaria.support.TestFixtures.area;
import static com.hotelaria.support.TestFixtures.booking;
import static com.hotelaria.support.TestFixtures.grant;
import static com.hotelaria.support.TestFixtures.guest;
import static com.hotelaria.support.TestFixtures.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {

    private static final LocalDateTime NOON = TODAY.atTime(12, 0);

    @Mock
    private BookingAreaAccessRepository accessRepository;

    private AccessPolicy policy;
    private Guest guest;
    private Room room;
    private HotelArea pool;

    @BeforeEach
    void setUp() {
        policy = new AccessPolicy(accessRepository);
        guest = guest(5L);
        room = room(1L, 2, "300.00");
        pool = area(7L, LocalTime.of(6, 0), LocalTime.of(22, 0));
    }

    private BookingAreaAccess stubGrant(BookingStatus bookingStatus) {
        Booking booking = booking(10L, guest, room, bookingStatus, TODAY.minusDays(1), TODAY.plusDays(2));
        BookingAreaAccess access = grant(1L, booking, pool);
        when(accessRepository.findByGuestIdAndAreaId(5L, 7L)).thenReturn(List.of(access));
        return access;
    }

    @Test
    @DisplayName("Permite acesso com autorização ativa, hospedagem em andamento e área aberta")
    void grantsAccess() {
        stubGrant(BookingStatus.CHECKED_IN);

        AccessDecision decision = policy.evaluate(5L, pool, NOON);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.booking().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Nega quando o hóspede não possui autorização para a área")
    void deniesWithoutAuthorization() {
        when(accessRepository.findByGuestIdAndAreaId(5L, 7L)).thenReturn(List.of());

        assertThat(policy.evaluate(5L, pool, NOON).denialReason()).isEqualTo(AccessDenialReason.NOT_AUTHORIZED);
    }

    @Test
    @DisplayName("Nega quando a autorização foi revogada")
    void deniesRevoked() {
        BookingAreaAccess access = stubGrant(BookingStatus.CHECKED_IN);
        access.revoke(null, NOON.minusHours(1));

        assertThat(policy.evaluate(5L, pool, NOON).denialReason())
                .isEqualTo(AccessDenialReason.AUTHORIZATION_REVOKED);
    }

    @Test
    @DisplayName("Nega quando a hospedagem foi cancelada")
    void deniesCancelledBooking() {
        stubGrant(BookingStatus.CANCELLED);

        assertThat(policy.evaluate(5L, pool, NOON).denialReason()).isEqualTo(AccessDenialReason.BOOKING_CANCELLED);
    }

    @Test
    @DisplayName("Nega quando o check-in ainda não foi realizado")
    void deniesBookingNotActive() {
        stubGrant(BookingStatus.CONFIRMED);

        assertThat(policy.evaluate(5L, pool, NOON).denialReason()).isEqualTo(AccessDenialReason.BOOKING_NOT_ACTIVE);
    }

    @Test
    @DisplayName("Nega fora do período da autorização")
    void deniesOutsidePeriod() {
        stubGrant(BookingStatus.CHECKED_IN);

        assertThat(policy.evaluate(5L, pool, TODAY.plusDays(10).atTime(12, 0)).denialReason())
                .isEqualTo(AccessDenialReason.OUTSIDE_AUTHORIZATION_PERIOD);
    }

    @Test
    @DisplayName("Nega quando a área está inativa, sem consultar autorizações")
    void deniesInactiveArea() {
        pool.setStatus(AreaStatus.INACTIVE);

        assertThat(policy.evaluate(5L, pool, NOON).denialReason()).isEqualTo(AccessDenialReason.AREA_UNAVAILABLE);
        verify(accessRepository, never()).findByGuestIdAndAreaId(any(), any());
    }

    @Test
    @DisplayName("Nega fora do horário de funcionamento da área")
    void deniesOutsideOpeningHours() {
        stubGrant(BookingStatus.CHECKED_IN);

        assertThat(policy.evaluate(5L, pool, TODAY.atTime(23, 30)).denialReason())
                .isEqualTo(AccessDenialReason.OUTSIDE_OPENING_HOURS);
    }

    @Test
    @DisplayName("Horário que atravessa a meia-noite é respeitado")
    void supportsOvernightHours() {
        HotelArea bar = area(8L, LocalTime.of(18, 0), LocalTime.of(2, 0));

        assertThat(bar.isOpenAt(LocalTime.of(23, 0))).isTrue();
        assertThat(bar.isOpenAt(LocalTime.of(1, 59))).isTrue();
        assertThat(bar.isOpenAt(LocalTime.of(12, 0))).isFalse();
    }
}
