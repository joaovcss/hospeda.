package com.hotelaria.repositories;

import com.hotelaria.models.Room;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByNumber(String number);

    boolean existsByNumberAndIdNot(String number, Long id);

    Page<Room> findByStatus(RoomStatus status, Pageable pageable);

    /**
     * Bloqueia a linha do quarto (SELECT ... FOR UPDATE) para serializar reservas
     * concorrentes do mesmo quarto até o fim da transação.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    /**
     * Quartos com capacidade suficiente, que não estão em manutenção/inativos e que não
     * possuem hospedagens bloqueantes sobrepostas ao período [checkIn, checkOut).
     */
    @Query("""
            select r from Room r
            where r.status not in :excludedStatuses
              and r.capacity >= :guests
              and not exists (
                  select b.id from Booking b
                  where b.room = r
                    and b.status in :blockingStatuses
                    and b.checkInDate < :checkOut
                    and b.checkOutDate > :checkIn)
            order by r.dailyRate, r.number
            """)
    List<Room> findAvailable(@Param("checkIn") LocalDate checkIn,
                             @Param("checkOut") LocalDate checkOut,
                             @Param("guests") int guests,
                             @Param("excludedStatuses") Collection<RoomStatus> excludedStatuses,
                             @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses);
}
