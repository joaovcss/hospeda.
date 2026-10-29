package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.RoomRequest;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.RoomResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.InvalidStatusTransitionException;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.RoomMapper;
import com.hotelaria.models.Room;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.RoomStatus;
import com.hotelaria.repositories.BookingRepository;
import com.hotelaria.repositories.RoomRepository;
import com.hotelaria.services.RoomService;
import com.hotelaria.services.support.BookingPeriodValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingPeriodValidator periodValidator;
    private final RoomMapper roomMapper;

    public RoomServiceImpl(RoomRepository roomRepository, BookingRepository bookingRepository,
                           BookingPeriodValidator periodValidator, RoomMapper roomMapper) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.periodValidator = periodValidator;
        this.roomMapper = roomMapper;
    }

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        String number = roomMapper.normalizeNumber(request.number());
        if (roomRepository.existsByNumber(number)) {
            throw new DuplicateResourceException("Já existe um quarto com o número " + number);
        }
        return roomMapper.toResponse(roomRepository.save(roomMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(RoomStatus status, Pageable pageable) {
        Page<Room> page = status == null
                ? roomRepository.findAll(pageable)
                : roomRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(roomMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse findById(Long id) {
        return roomMapper.toResponse(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findRoom(id);
        String number = roomMapper.normalizeNumber(request.number());
        if (roomRepository.existsByNumberAndIdNot(number, id)) {
            throw new DuplicateResourceException("Já existe um quarto com o número " + number);
        }
        roomMapper.updateEntity(room, request);
        return roomMapper.toResponse(room);
    }

    /**
     * Alteração manual de status. OCCUPIED é controlado pelo check-in/check-out, e não
     * é possível alterar um quarto com hóspede hospedado nem inativar quarto com reservas futuras.
     */
    @Override
    @Transactional
    public RoomResponse updateStatus(Long id, RoomStatus status) {
        if (status == RoomStatus.OCCUPIED) {
            throw new BusinessRuleException("O status OCCUPIED é definido automaticamente pelo check-in");
        }
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto", id));
        if (room.getStatus() == status) {
            return roomMapper.toResponse(room);
        }
        if (bookingRepository.existsByRoom_IdAndStatus(id, BookingStatus.CHECKED_IN)) {
            throw new InvalidStatusTransitionException(
                    "O quarto possui hóspede em check-in; realize o check-out antes de alterar o status");
        }
        if (status == RoomStatus.INACTIVE && bookingRepository.existsByRoom_IdAndStatusInAndCheckOutDateAfter(
                id, EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), periodValidator.today())) {
            throw new InvalidStatusTransitionException(
                    "O quarto possui reservas futuras; cancele-as antes de inativá-lo");
        }
        room.setStatus(status);
        return roomMapper.toResponse(room);
    }

    /**
     * Disponibilidade calculada pelas datas das hospedagens confirmadas/em andamento,
     * e não apenas pelo status atual do quarto.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> findAvailable(LocalDate checkIn, LocalDate checkOut, int guests) {
        periodValidator.validate(checkIn, checkOut);
        if (guests < 1) {
            throw new BusinessRuleException("A quantidade de hóspedes deve ser maior que zero");
        }
        return roomRepository.findAvailable(checkIn, checkOut, guests,
                        EnumSet.of(RoomStatus.MAINTENANCE, RoomStatus.INACTIVE), BookingStatus.BLOCKING)
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quarto", id));
    }
}
