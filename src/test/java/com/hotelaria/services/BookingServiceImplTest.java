package com.hotelaria.services;

import com.hotelaria.dtos.request.BookingCreateRequest;
import com.hotelaria.dtos.response.BookingResponse;
import com.hotelaria.exceptions.BookingConflictException;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.exceptions.InvalidBookingDatesException;
import com.hotelaria.exceptions.InvalidStatusTransitionException;
import com.hotelaria.exceptions.RoomUnavailableException;
import com.hotelaria.mappers.BookingMapper;
import com.hotelaria.models.Booking;
import com.hotelaria.models.Guest;
import com.hotelaria.models.Room;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.RoomStatus;
import com.hotelaria.repositories.BookingRepository;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.RoomRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.impl.BookingServiceImpl;
import com.hotelaria.services.support.BookingPeriodValidator;
import com.hotelaria.services.support.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.hotelaria.support.TestFixtures.CLOCK;
import static com.hotelaria.support.TestFixtures.TODAY;
import static com.hotelaria.support.TestFixtures.booking;
import static com.hotelaria.support.TestFixtures.employee;
import static com.hotelaria.support.TestFixtures.guest;
import static com.hotelaria.support.TestFixtures.guestActor;
import static com.hotelaria.support.TestFixtures.room;
import static com.hotelaria.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private GuestRepository guestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private BookingServiceImpl service;
    private Guest guest;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookingRepository, roomRepository, guestRepository, userRepository,
                currentUserProvider, new OwnershipGuard(), new BookingPeriodValidator(CLOCK),
                new BookingMapper(), CLOCK);
        guest = guest(5L);
        room = room(1L, 2, "300.00");
    }

    private BookingCreateRequest request(Long guestId, int nights, int guests) {
        return new BookingCreateRequest(guestId, 1L, TODAY.plusDays(1), TODAY.plusDays(1 + nights), guests, null);
    }

    private void stubSuccessfulCreation(Long actorUserId) {
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapping(eq(1L), any(), any(), any(), eq(0L))).thenReturn(false);
        when(userRepository.getReferenceById(actorUserId)).thenReturn(user(actorUserId,
                com.hotelaria.models.enums.RoleName.ROLE_EMPLOYEE, "x@x.com"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Funcionário cria reserva já confirmada com valor total calculado")
    void employeeCreatesConfirmedBooking() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        stubSuccessfulCreation(2L);

        BookingResponse response = service.create(request(5L, 3, 2));

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(response.confirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("Hóspede cria solicitação PENDING sempre para si mesmo")
    void guestCreatesPendingRequest() {
        when(currentUserProvider.get()).thenReturn(guestActor(5L));
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        stubSuccessfulCreation(105L);

        BookingResponse response = service.create(request(null, 2, 1));

        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.guestId()).isEqualTo(5L);
        assertThat(response.confirmedAt()).isNull();
    }

    @Test
    @DisplayName("Hóspede não pode solicitar hospedagem em nome de outro hóspede")
    void guestCannotBookForAnotherGuest() {
        when(currentUserProvider.get()).thenReturn(guestActor(5L));

        assertThatThrownBy(() -> service.create(request(6L, 2, 1)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Data de saída deve ser posterior à data de entrada")
    void rejectsInvalidDates() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        BookingCreateRequest invalid = new BookingCreateRequest(5L, 1L, TODAY.plusDays(3), TODAY.plusDays(3), 1, null);

        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(InvalidBookingDatesException.class);
    }

    @Test
    @DisplayName("Data de entrada no passado é rejeitada")
    void rejectsPastCheckIn() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        BookingCreateRequest past = new BookingCreateRequest(5L, 1L, TODAY.minusDays(1), TODAY.plusDays(2), 1, null);

        assertThatThrownBy(() -> service.create(past)).isInstanceOf(InvalidBookingDatesException.class);
    }

    @Test
    @DisplayName("Quantidade de hóspedes não pode exceder a capacidade do quarto")
    void rejectsCapacityExceeded() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.create(request(5L, 2, 3))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Quarto em manutenção não aceita hospedagens")
    void rejectsRoomInMaintenance() {
        room.setStatus(RoomStatus.MAINTENANCE);
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.create(request(5L, 2, 1))).isInstanceOf(RoomUnavailableException.class);
    }

    @Test
    @DisplayName("Reserva conflitante com outra confirmada no mesmo quarto é rejeitada")
    void rejectsConflictingBooking() {
        when(currentUserProvider.get()).thenReturn(employee());
        when(guestRepository.findById(5L)).thenReturn(Optional.of(guest));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapping(eq(1L), any(), any(), eq(BookingStatus.BLOCKING), eq(0L)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request(5L, 2, 1))).isInstanceOf(BookingConflictException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Confirmação também verifica conflito, ignorando a própria reserva")
    void confirmChecksConflict() {
        Booking pending = booking(10L, guest, room, BookingStatus.PENDING, TODAY.plusDays(1), TODAY.plusDays(3));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(pending));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapping(eq(1L), any(), any(), any(), eq(10L))).thenReturn(true);

        assertThatThrownBy(() -> service.confirm(10L)).isInstanceOf(BookingConflictException.class);
        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Confirmação de reserva PENDING sem conflito")
    void confirmPendingBooking() {
        Booking pending = booking(10L, guest, room, BookingStatus.PENDING, TODAY.plusDays(1), TODAY.plusDays(3));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(pending));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlapping(eq(1L), any(), any(), any(), eq(10L))).thenReturn(false);

        assertThat(service.confirm(10L).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Hóspede não consegue consultar hospedagem de outra pessoa")
    void guestCannotReadOthersBooking() {
        Booking othersBooking = booking(10L, guest(6L), room, BookingStatus.CONFIRMED, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(othersBooking));
        when(currentUserProvider.get()).thenReturn(guestActor(5L));

        assertThatThrownBy(() -> service.findById(10L)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Hóspede consulta a própria hospedagem")
    void guestReadsOwnBooking() {
        Booking own = booking(10L, guest, room, BookingStatus.CONFIRMED, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(own));
        when(currentUserProvider.get()).thenReturn(guestActor(5L));

        assertThat(service.findById(10L).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Não é permitido check-in de reserva cancelada")
    void cannotCheckInCancelled() {
        Booking cancelled = booking(10L, guest, room, BookingStatus.CANCELLED, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.checkIn(10L)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Não é permitido check-in de reserva ainda pendente")
    void cannotCheckInPending() {
        Booking pending = booking(10L, guest, room, BookingStatus.PENDING, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.checkIn(10L)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Check-in antes da data de entrada é rejeitado")
    void cannotCheckInBeforeDate() {
        Booking future = booking(10L, guest, room, BookingStatus.CONFIRMED, TODAY.plusDays(2), TODAY.plusDays(4));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> service.checkIn(10L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Check-in marca o quarto como ocupado; check-out o libera")
    void checkInAndCheckOutUpdateRoom() {
        Booking confirmed = booking(10L, guest, room, BookingStatus.CONFIRMED, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(confirmed));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));

        BookingResponse checkedIn = service.checkIn(10L);
        assertThat(checkedIn.status()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(checkedIn.checkedInAt()).isNotNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);

        BookingResponse checkedOut = service.checkOut(10L);
        assertThat(checkedOut.status()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Não é permitido check-out sem check-in")
    void cannotCheckOutWithoutCheckIn() {
        Booking confirmed = booking(10L, guest, room, BookingStatus.CONFIRMED, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> service.checkOut(10L)).isInstanceOf(InvalidStatusTransitionException.class);
        verify(roomRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("Hospedagem finalizada não pode ser reaberta nem cancelada")
    void finishedBookingIsFinal() {
        Booking finished = booking(10L, guest, room, BookingStatus.CHECKED_OUT, TODAY.minusDays(3), TODAY);
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(finished));
        when(currentUserProvider.get()).thenReturn(employee());

        assertThatThrownBy(() -> service.checkIn(10L)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> service.cancel(10L, "x")).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> service.confirm(10L)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Hospedagem em andamento não pode ser cancelada")
    void cannotCancelCheckedIn() {
        Booking checkedIn = booking(10L, guest, room, BookingStatus.CHECKED_IN, TODAY, TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(checkedIn));
        when(currentUserProvider.get()).thenReturn(employee());

        assertThatThrownBy(() -> service.cancel(10L, null)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Hóspede pode cancelar a própria reserva, mas não a de outra pessoa")
    void guestCancellationRespectsOwnership() {
        Booking own = booking(10L, guest, room, BookingStatus.CONFIRMED, TODAY.plusDays(1), TODAY.plusDays(2));
        Booking others = booking(11L, guest(6L), room, BookingStatus.CONFIRMED, TODAY.plusDays(1), TODAY.plusDays(2));
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(own));
        when(bookingRepository.findDetailedById(11L)).thenReturn(Optional.of(others));
        when(currentUserProvider.get()).thenReturn(guestActor(5L));

        assertThat(service.cancel(10L, "Mudança de planos").status()).isEqualTo(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(11L, null)).isInstanceOf(ForbiddenOperationException.class);
        assertThat(others.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
