package com.hotelaria.services;

import com.hotelaria.dtos.request.RoomRequest;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.InvalidBookingDatesException;
import com.hotelaria.exceptions.InvalidStatusTransitionException;
import com.hotelaria.mappers.RoomMapper;
import com.hotelaria.models.Room;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.RoomCategory;
import com.hotelaria.models.enums.RoomStatus;
import com.hotelaria.repositories.BookingRepository;
import com.hotelaria.repositories.RoomRepository;
import com.hotelaria.services.impl.RoomServiceImpl;
import com.hotelaria.services.support.BookingPeriodValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.hotelaria.support.TestFixtures.CLOCK;
import static com.hotelaria.support.TestFixtures.TODAY;
import static com.hotelaria.support.TestFixtures.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BookingRepository bookingRepository;

    private RoomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoomServiceImpl(roomRepository, bookingRepository, new BookingPeriodValidator(CLOCK),
                new RoomMapper());
    }

    @Test
    @DisplayName("Cadastro de quarto normaliza o número e inicia como AVAILABLE")
    void createsRoom() {
        when(roomRepository.existsByNumber("101A")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(new RoomRequest(" 101a ", 1, 2, RoomCategory.DELUXE,
                new BigDecimal("450.00"), null));

        assertThat(response.number()).isEqualTo("101A");
        assertThat(response.status()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Número de quarto duplicado é rejeitado")
    void rejectsDuplicateNumber() {
        when(roomRepository.existsByNumber("101")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new RoomRequest("101", 1, 2, RoomCategory.STANDARD,
                new BigDecimal("200.00"), null))).isInstanceOf(DuplicateResourceException.class);
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Disponibilidade valida o período informado")
    void availabilityRejectsInvalidPeriod() {
        assertThatThrownBy(() -> service.findAvailable(TODAY.plusDays(5), TODAY.plusDays(2), 1))
                .isInstanceOf(InvalidBookingDatesException.class);
    }

    @Test
    @DisplayName("Disponibilidade considera datas das hospedagens bloqueantes e exclui manutenção/inativos")
    void availabilityUsesBookingDates() {
        when(roomRepository.findAvailable(TODAY.plusDays(1), TODAY.plusDays(3), 2,
                EnumSet.of(RoomStatus.MAINTENANCE, RoomStatus.INACTIVE), BookingStatus.BLOCKING))
                .thenReturn(List.of(room(1L, 2, "300.00")));

        assertThat(service.findAvailable(TODAY.plusDays(1), TODAY.plusDays(3), 2)).hasSize(1);
    }

    @Test
    @DisplayName("Status OCCUPIED não pode ser definido manualmente")
    void cannotSetOccupiedManually() {
        assertThatThrownBy(() -> service.updateStatus(1L, RoomStatus.OCCUPIED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Quarto com hóspede em check-in não pode ter o status alterado")
    void cannotChangeStatusWhileCheckedIn() {
        Room occupied = room(1L, 2, "300.00");
        occupied.setStatus(RoomStatus.OCCUPIED);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(occupied));
        when(bookingRepository.existsByRoom_IdAndStatus(1L, BookingStatus.CHECKED_IN)).thenReturn(true);

        assertThatThrownBy(() -> service.updateStatus(1L, RoomStatus.MAINTENANCE))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
