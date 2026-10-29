package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.BookingCreateRequest;
import com.hotelaria.dtos.request.BookingScope;
import com.hotelaria.dtos.response.BookingResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.exceptions.BookingConflictException;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.exceptions.InvalidBookingDatesException;
import com.hotelaria.exceptions.InvalidStatusTransitionException;
import com.hotelaria.exceptions.ResourceNotFoundException;
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
import com.hotelaria.repositories.specifications.BookingSpecifications;
import com.hotelaria.security.AuthenticatedUser;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.BookingService;
import com.hotelaria.services.support.BookingPeriodValidator;
import com.hotelaria.services.support.OwnershipGuard;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Regras de hospedagem.
 * <ul>
 *   <li>Hóspedes criam solicitações PENDING; funcionários/administradores criam reservas CONFIRMED.</li>
 *   <li>Reservas confirmadas não podem se sobrepor no mesmo quarto. A verificação é feita com
 *       lock pessimista na linha do quarto e garantida pela exclusion constraint do banco.</li>
 *   <li>Transições: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT; PENDING/CONFIRMED → CANCELLED.
 *       CHECKED_OUT e CANCELLED são finais.</li>
 * </ul>
 */
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OwnershipGuard ownershipGuard;
    private final BookingPeriodValidator periodValidator;
    private final BookingMapper bookingMapper;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookingRepository, RoomRepository roomRepository,
                              GuestRepository guestRepository, UserRepository userRepository,
                              CurrentUserProvider currentUserProvider, OwnershipGuard ownershipGuard,
                              BookingPeriodValidator periodValidator, BookingMapper bookingMapper, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.ownershipGuard = ownershipGuard;
        this.periodValidator = periodValidator;
        this.bookingMapper = bookingMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingResponse create(BookingCreateRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        Guest guest = resolveGuest(request.guestId(), actor);
        periodValidator.validate(request.checkInDate(), request.checkOutDate());

        Room room = lockRoom(request.roomId());
        ensureRoomCanHost(room, request.numberOfGuests());
        ensureNoConflict(room.getId(), request.checkInDate(), request.checkOutDate(), 0L);

        BookingStatus initialStatus = actor.isGuest() ? BookingStatus.PENDING : BookingStatus.CONFIRMED;
        long nights = request.checkOutDate().toEpochDay() - request.checkInDate().toEpochDay();
        BigDecimal total = room.getDailyRate().multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking(guest, room, userRepository.getReferenceById(actor.getId()),
                request.checkInDate(), request.checkOutDate(), request.numberOfGuests(),
                initialStatus, total, request.notes());
        if (initialStatus == BookingStatus.CONFIRMED) {
            booking.setConfirmedAt(now());
        }
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> list(Long guestId, Long roomId, BookingStatus status,
                                              LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Booking> spec = Specification.allOf(
                BookingSpecifications.hasGuest(guestId),
                BookingSpecifications.hasRoom(roomId),
                BookingSpecifications.hasStatus(status),
                BookingSpecifications.overlapsPeriod(from, to));
        return PageResponse.from(bookingRepository.findAll(spec, pageable).map(bookingMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        Booking booking = findBooking(id);
        ownershipGuard.ensureCanAccessBooking(currentUserProvider.get(), booking);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listMine(BookingScope scope, Pageable pageable) {
        Long guestId = ownershipGuard.requireGuestId(currentUserProvider.get());
        LocalDate today = periodValidator.today();
        Specification<Booking> scopeSpec = switch (scope == null ? BookingScope.ALL : scope) {
            case CURRENT -> BookingSpecifications.isCurrent(today);
            case PAST -> BookingSpecifications.isPast(today);
            case ALL -> null;
        };
        Specification<Booking> spec = Specification.allOf(BookingSpecifications.hasGuest(guestId), scopeSpec);
        return PageResponse.from(bookingRepository.findAll(spec, pageable).map(bookingMapper::toResponse));
    }

    @Override
    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = findBooking(id);
        ensureTransition(booking, BookingStatus.CONFIRMED);
        if (booking.getCheckInDate().isBefore(periodValidator.today())) {
            throw new InvalidBookingDatesException(
                    "A data de entrada desta solicitação já passou; ela deve ser cancelada");
        }
        Room room = lockRoom(booking.getRoom().getId());
        ensureRoomCanHost(room, booking.getNumberOfGuests());
        ensureNoConflict(room.getId(), booking.getCheckInDate(), booking.getCheckOutDate(), booking.getId());

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now());
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse checkIn(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Não é possível realizar check-in de uma reserva cancelada");
        }
        ensureTransition(booking, BookingStatus.CHECKED_IN);

        LocalDate today = periodValidator.today();
        if (today.isBefore(booking.getCheckInDate()) || !today.isBefore(booking.getCheckOutDate())) {
            throw new BusinessRuleException("O check-in só pode ser realizado entre "
                    + booking.getCheckInDate() + " e o dia anterior à saída (" + booking.getCheckOutDate() + ")");
        }
        Room room = lockRoom(booking.getRoom().getId());
        if (!room.isBookable()) {
            throw new RoomUnavailableException("O quarto " + room.getNumber() + " está indisponível ("
                    + room.getStatus() + ")");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(now());
        room.setStatus(RoomStatus.OCCUPIED);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse checkOut(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new InvalidStatusTransitionException(
                    "O check-out só pode ser realizado em hospedagens com check-in (status atual: "
                            + booking.getStatus() + ")");
        }
        Room room = lockRoom(booking.getRoom().getId());
        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(now());
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long id, String reason) {
        Booking booking = findBooking(id);
        ownershipGuard.ensureCanAccessBooking(currentUserProvider.get(), booking);
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new InvalidStatusTransitionException(
                    "Hospedagem em andamento não pode ser cancelada; realize o check-out");
        }
        ensureTransition(booking, BookingStatus.CANCELLED);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now());
        booking.setCancellationReason(reason);
        return bookingMapper.toResponse(booking);
    }

    /**
     * Hóspedes sempre reservam para si mesmos; o guestId do corpo nunca é usado para
     * escolher outra pessoa. Funcionários precisam informar o hóspede.
     */
    private Guest resolveGuest(Long requestedGuestId, AuthenticatedUser actor) {
        if (actor.isGuest()) {
            Long ownGuestId = ownershipGuard.requireGuestId(actor);
            if (requestedGuestId != null && !requestedGuestId.equals(ownGuestId)) {
                throw new ForbiddenOperationException("Hóspedes só podem solicitar hospedagens para si mesmos");
            }
            return guestRepository.findById(ownGuestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hóspede", ownGuestId));
        }
        if (requestedGuestId == null) {
            throw new BusinessRuleException("Informe o hóspede (guestId) da hospedagem");
        }
        return guestRepository.findById(requestedGuestId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóspede", requestedGuestId));
    }

    private void ensureRoomCanHost(Room room, int numberOfGuests) {
        if (!room.isBookable()) {
            throw new RoomUnavailableException("O quarto " + room.getNumber() + " está indisponível ("
                    + room.getStatus() + ")");
        }
        if (numberOfGuests > room.getCapacity()) {
            throw new BusinessRuleException("O quarto " + room.getNumber() + " comporta no máximo "
                    + room.getCapacity() + " hóspede(s)");
        }
    }

    private void ensureNoConflict(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludeBookingId) {
        if (bookingRepository.existsOverlapping(roomId, checkIn, checkOut, BookingStatus.BLOCKING, excludeBookingId)) {
            throw new BookingConflictException();
        }
    }

    private void ensureTransition(Booking booking, BookingStatus target) {
        if (!booking.getStatus().canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(booking.getStatus(), target);
        }
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto", roomId));
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospedagem", id));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
