package com.hotelaria.support;

import com.hotelaria.models.Booking;
import com.hotelaria.models.BookingAreaAccess;
import com.hotelaria.models.Guest;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.Role;
import com.hotelaria.models.Room;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.models.enums.RoomCategory;
import com.hotelaria.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

/**
 * Objetos de apoio para os testes unitários.
 */
public final class TestFixtures {

    /** 2026-11-10 12:00 no fuso do hotel. */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    public static final LocalDate TODAY = LocalDate.now(CLOCK);

    private TestFixtures() {
    }

    public static AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin@hotel.com", "Admin", RoleName.ROLE_ADMIN, null,
                EnumSet.allOf(PermissionName.class));
    }

    public static AuthenticatedUser employee(PermissionName... extra) {
        Set<PermissionName> permissions = EnumSet.of(PermissionName.BOOKING_CREATE, PermissionName.CHECKIN,
                PermissionName.CHECKOUT, PermissionName.GUEST_CREATE);
        permissions.addAll(Set.of(extra));
        return new AuthenticatedUser(2L, "func@hotel.com", "Funcionário", RoleName.ROLE_EMPLOYEE, null, permissions);
    }

    public static AuthenticatedUser guestActor(Long guestId) {
        return new AuthenticatedUser(100L + guestId, "hospede" + guestId + "@mail.com", "Hóspede " + guestId,
                RoleName.ROLE_GUEST, guestId, EnumSet.of(PermissionName.AREA_READ));
    }

    public static Role role(RoleName name) {
        return new Role(name, name.name());
    }

    public static User user(Long id, RoleName roleName, String email) {
        User user = new User("Usuário " + id, email, "hash", role(roleName));
        user.setId(id);
        return user;
    }

    public static Guest guest(Long id) {
        User user = user(100L + id, RoleName.ROLE_GUEST, "hospede" + id + "@mail.com");
        Guest guest = new Guest(user, "5298224702" + (id % 10), "11999990000", null, null);
        guest.setId(id);
        return guest;
    }

    public static Room room(Long id, int capacity, String dailyRate) {
        Room room = new Room("10" + id, 1, capacity, RoomCategory.STANDARD, new BigDecimal(dailyRate), null);
        room.setId(id);
        return room;
    }

    public static Booking booking(Long id, Guest guest, Room room, BookingStatus status,
                                  LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking(guest, room, guest.getUser(), checkIn, checkOut, 1, status,
                room.getDailyRate(), null);
        booking.setId(id);
        return booking;
    }

    public static HotelArea area(Long id, LocalTime opening, LocalTime closing) {
        HotelArea area = new HotelArea("Área " + id, null, "Térreo", opening, closing);
        area.setId(id);
        return area;
    }

    public static BookingAreaAccess grant(Long id, Booking booking, HotelArea area) {
        BookingAreaAccess access = new BookingAreaAccess(booking, area,
                booking.getCheckInDate().atStartOfDay(), booking.getCheckOutDate().plusDays(1).atStartOfDay(),
                booking.getCreatedBy());
        access.setId(id);
        return access;
    }
}
