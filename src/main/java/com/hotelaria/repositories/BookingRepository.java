package com.hotelaria.repositories;

import com.hotelaria.models.Booking;
import com.hotelaria.models.enums.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @EntityGraph(attributePaths = {"guest", "guest.user", "room"})
    Optional<Booking> findDetailedById(Long id);

    /**
     * Verifica se existe outra hospedagem com status bloqueante no mesmo quarto
     * cujo período [checkIn, checkOut) se sobreponha ao informado.
     */
    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId
              and b.id <> :excludeBookingId
              and b.status in :statuses
              and b.checkInDate < :checkOut
              and b.checkOutDate > :checkIn
            """)
    boolean existsOverlapping(@Param("roomId") Long roomId,
                              @Param("checkIn") LocalDate checkIn,
                              @Param("checkOut") LocalDate checkOut,
                              @Param("statuses") Collection<BookingStatus> statuses,
                              @Param("excludeBookingId") Long excludeBookingId);

    boolean existsByRoom_IdAndStatus(Long roomId, BookingStatus status);

    boolean existsByRoom_IdAndStatusInAndCheckOutDateAfter(Long roomId, Collection<BookingStatus> statuses,
                                                           LocalDate date);
}
