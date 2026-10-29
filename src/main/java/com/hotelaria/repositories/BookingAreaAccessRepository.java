package com.hotelaria.repositories;

import com.hotelaria.models.BookingAreaAccess;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingAreaAccessRepository extends JpaRepository<BookingAreaAccess, Long> {

    Optional<BookingAreaAccess> findByBooking_IdAndArea_Id(Long bookingId, Long areaId);

    @EntityGraph(attributePaths = "area")
    List<BookingAreaAccess> findByBooking_IdOrderByArea_NameAsc(Long bookingId);

    @Query("""
            select a from BookingAreaAccess a
            join fetch a.booking b
            join fetch a.area
            where b.guest.id = :guestId
            order by b.checkInDate desc, a.area.name
            """)
    List<BookingAreaAccess> findByGuestId(@Param("guestId") Long guestId);

    /** Autorizações do hóspede para a área, da hospedagem mais recente para a mais antiga. */
    @Query("""
            select a from BookingAreaAccess a
            join fetch a.booking b
            where b.guest.id = :guestId
              and a.area.id = :areaId
            order by b.checkInDate desc
            """)
    List<BookingAreaAccess> findByGuestIdAndAreaId(@Param("guestId") Long guestId,
                                                   @Param("areaId") Long areaId);
}
