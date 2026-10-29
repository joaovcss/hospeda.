package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.AccessValidationRequest;
import com.hotelaria.dtos.response.AccessLogResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.AccessMapper;
import com.hotelaria.models.AccessLog;
import com.hotelaria.models.Guest;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.enums.AccessResult;
import com.hotelaria.repositories.AccessLogRepository;
import com.hotelaria.repositories.GuestRepository;
import com.hotelaria.repositories.HotelAreaRepository;
import com.hotelaria.repositories.UserRepository;
import com.hotelaria.repositories.specifications.AccessLogSpecifications;
import com.hotelaria.security.AuthenticatedUser;
import com.hotelaria.security.CurrentUserProvider;
import com.hotelaria.services.AccessLogService;
import com.hotelaria.services.support.AccessDecision;
import com.hotelaria.services.support.AccessPolicy;
import com.hotelaria.services.support.OwnershipGuard;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final GuestRepository guestRepository;
    private final HotelAreaRepository areaRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OwnershipGuard ownershipGuard;
    private final AccessPolicy accessPolicy;
    private final AccessMapper accessMapper;
    private final Clock clock;

    public AccessLogServiceImpl(AccessLogRepository accessLogRepository, GuestRepository guestRepository,
                                HotelAreaRepository areaRepository, UserRepository userRepository,
                                CurrentUserProvider currentUserProvider, OwnershipGuard ownershipGuard,
                                AccessPolicy accessPolicy, AccessMapper accessMapper, Clock clock) {
        this.accessLogRepository = accessLogRepository;
        this.guestRepository = guestRepository;
        this.areaRepository = areaRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.ownershipGuard = ownershipGuard;
        this.accessPolicy = accessPolicy;
        this.accessMapper = accessMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccessLogResponse validateAndRegister(AccessValidationRequest request) {
        AuthenticatedUser actor = currentUserProvider.get();
        Guest guest = guestRepository.findWithUserById(request.guestId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóspede", request.guestId()));
        HotelArea area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área", request.areaId()));

        LocalDateTime now = LocalDateTime.now(clock);
        AccessDecision decision = accessPolicy.evaluate(guest.getId(), area, now);
        AccessLog log = new AccessLog(guest, decision.booking(), area, now,
                decision.allowed() ? AccessResult.GRANTED : AccessResult.DENIED,
                decision.denialReason(),
                userRepository.getReferenceById(actor.getId()));
        AccessLog saved = accessLogRepository.save(log);
        return new AccessLogResponse(
                saved.getId(), guest.getId(), guest.getUser().getFullName(),
                decision.booking() == null ? null : decision.booking().getId(),
                area.getId(), area.getName(), now, saved.getResult(), saved.getDenialReason(),
                saved.getDenialReason() == null ? null : saved.getDenialReason().getDescription(),
                actor.getId(), actor.getFullName());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccessLogResponse> list(Long guestId, Long areaId, Long bookingId, AccessResult result,
                                                LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<AccessLog> spec = Specification.allOf(
                AccessLogSpecifications.hasGuest(guestId),
                AccessLogSpecifications.hasArea(areaId),
                AccessLogSpecifications.hasBooking(bookingId),
                AccessLogSpecifications.hasResult(result),
                AccessLogSpecifications.attemptedFrom(from),
                AccessLogSpecifications.attemptedUntil(to));
        return PageResponse.from(accessLogRepository.findAll(spec, pageable).map(accessMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccessLogResponse> listMine(Pageable pageable) {
        Long guestId = ownershipGuard.requireGuestId(currentUserProvider.get());
        return PageResponse.from(accessLogRepository.findAll(AccessLogSpecifications.hasGuest(guestId), pageable)
                .map(accessMapper::toResponse));
    }
}
