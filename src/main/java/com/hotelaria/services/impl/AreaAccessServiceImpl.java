package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.AreaAccessGrantRequest;
import com.hotelaria.dtos.response.AccessCheckResponse;
import com.hotelaria.dtos.response.BookingAreaAccessResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.InvalidStatusTransitionException;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.AccessMapper;
import com.hotelaria.models.Booking;
import com.hotelaria.models.BookingAreaAccess;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.AccessGrantStatus;
import com.hotelaria.models.enums.AreaStatus;
import com.hotelaria.repositories.BookingAreaAccessRepository;
import com.hotelaria.repositories.BookingRepository;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.HotelAreaRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.security.AuthenticatedUser;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.AreaAccessService;
import com.hotelaria.services.support.AccessDecision;
import com.hotelaria.services.support.AccessPolicy;
import com.hotelaria.services.support.OwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AreaAccessServiceImpl implements AreaAccessService {

    private final BookingAreaAccessRepository accessRepository;
    private final BookingRepository bookingRepository;
    private final HotelAreaRepository areaRepository;
    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OwnershipGuard ownershipGuard;
    private final AccessPolicy accessPolicy;
    private final AccessMapper accessMapper;
    private final Clock clock;

    public AreaAccessServiceImpl(BookingAreaAccessRepository accessRepository, BookingRepository bookingRepository,
                                 HotelAreaRepository areaRepository, GuestRepository guestRepository,
                                 UserRepository userRepository, CurrentUserProvider currentUserProvider,
                                 OwnershipGuard ownershipGuard, AccessPolicy accessPolicy,
                                 AccessMapper accessMapper, Clock clock) {
        this.accessRepository = accessRepository;
        this.bookingRepository = bookingRepository;
        this.areaRepository = areaRepository;
        this.guestRepository = guestRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.ownershipGuard = ownershipGuard;
        this.accessPolicy = accessPolicy;
        this.accessMapper = accessMapper;
        this.clock = clock;
    }

    /**
     * Concede acesso de uma hospedagem a uma área. A janela padrão cobre todo o período
     * da hospedagem e uma janela personalizada precisa estar contida nele.
     */
    @Override
    @Transactional
    public BookingAreaAccessResponse grant(Long bookingId, AreaAccessGrantRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        Booking booking = findBooking(bookingId);
        if (booking.getStatus().isFinal()) {
            throw new InvalidStatusTransitionException(
                    "Não é possível conceder acesso a uma hospedagem " + booking.getStatus());
        }
        HotelArea area = findArea(request.areaId());
        if (area.getStatus() == AreaStatus.INACTIVE) {
            throw new BusinessRuleException("Não é possível conceder acesso a uma área inativa");
        }

        LocalDateTime bookingStart = booking.getCheckInDate().atStartOfDay();
        LocalDateTime bookingEnd = booking.getCheckOutDate().plusDays(1).atStartOfDay();
        LocalDateTime validFrom = request.validFrom() != null ? request.validFrom() : bookingStart;
        LocalDateTime validUntil = request.validUntil() != null ? request.validUntil() : bookingEnd;
        if (!validUntil.isAfter(validFrom)) {
            throw new BusinessRuleException("O fim da autorização deve ser posterior ao início");
        }
        if (validFrom.isBefore(bookingStart) || validUntil.isAfter(bookingEnd)) {
            throw new BusinessRuleException("A autorização deve estar dentro do período da hospedagem ("
                    + bookingStart + " a " + bookingEnd + ")");
        }

        User grantedBy = userRepository.getReferenceById(actor.getId());
        Optional<BookingAreaAccess> existing = accessRepository.findByBooking_IdAndArea_Id(bookingId, area.getId());
        BookingAreaAccess access;
        if (existing.isPresent()) {
            access = existing.get();
            if (access.getStatus() == AccessGrantStatus.ACTIVE) {
                throw new DuplicateResourceException("A hospedagem já possui acesso ativo a esta área");
            }
            access.reactivate(validFrom, validUntil, grantedBy);
        } else {
            access = accessRepository.save(new BookingAreaAccess(booking, area, validFrom, validUntil, grantedBy));
        }
        return accessMapper.toResponse(access);
    }

    /** Revogação idempotente: revogar um acesso já revogado não gera erro. */
    @Override
    @Transactional
    public void revoke(Long bookingId, Long areaId) {
        AuthenticatedUser actor = currentUserProvider.get();
        BookingAreaAccess access = accessRepository.findByBooking_IdAndArea_Id(bookingId, areaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Autorização de acesso não encontrada para a hospedagem e área informadas"));
        if (access.getStatus() == AccessGrantStatus.ACTIVE) {
            access.revoke(userRepository.getReferenceById(actor.getId()), LocalDateTime.now(clock));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingAreaAccessResponse> listByBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        ownershipGuard.ensureCanAccessBooking(currentUserProvider.get(), booking);
        return accessRepository.findByBooking_IdOrderByArea_NameAsc(bookingId).stream()
                .map(accessMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingAreaAccessResponse> listByGuest(Long guestId) {
        if (!guestRepository.existsById(guestId)) {
            throw new ResourceNotFoundException("Hóspede", guestId);
        }
        return accessRepository.findByGuestId(guestId).stream()
                .map(accessMapper::toResponse)
                .toList();
    }

    /** Autorizações ativas do hóspede autenticado em hospedagens não finalizadas. */
    @Override
    @Transactional(readOnly = true)
    public List<BookingAreaAccessResponse> listMine() {
        Long guestId = ownershipGuard.requireGuestId(currentUserProvider.get());
        return accessRepository.findByGuestId(guestId).stream()
                .filter(access -> access.getStatus() == AccessGrantStatus.ACTIVE)
                .filter(access -> !access.getBooking().getStatus().isFinal())
                .map(accessMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccessCheckResponse check(Long guestId, Long areaId) {
        if (!guestRepository.existsById(guestId)) {
            throw new ResourceNotFoundException("Hóspede", guestId);
        }
        HotelArea area = findArea(areaId);
        AccessDecision decision = accessPolicy.evaluate(guestId, area, LocalDateTime.now(clock));
        return new AccessCheckResponse(
                guestId,
                areaId,
                decision.allowed(),
                decision.booking() == null ? null : decision.booking().getId(),
                decision.denialReason(),
                decision.denialReason() == null ? null : decision.denialReason().getDescription());
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hospedagem", id));
    }

    private HotelArea findArea(Long id) {
        return areaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Área", id));
    }
}
