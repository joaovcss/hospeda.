package com.hotelaria.models;

import com.hotelaria.models.enums.AccessDenialReason;
import com.hotelaria.models.enums.AccessResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro imutável de uma tentativa de acesso a uma área do hotel.
 */
@Entity
@Table(name = "access_logs")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    /** Hospedagem avaliada na tentativa; nula quando não há autorização. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private HotelArea area;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccessResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "denial_reason", length = 40)
    private AccessDenialReason denialReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_user_id")
    private User registeredBy;

    protected AccessLog() {
    }

    public AccessLog(Guest guest, Booking booking, HotelArea area, LocalDateTime attemptedAt,
                     AccessResult result, AccessDenialReason denialReason, User registeredBy) {
        this.guest = guest;
        this.booking = booking;
        this.area = area;
        this.attemptedAt = attemptedAt;
        this.result = result;
        this.denialReason = denialReason;
        this.registeredBy = registeredBy;
    }

    public Long getId() {
        return id;
    }

    public Guest getGuest() {
        return guest;
    }

    public Booking getBooking() {
        return booking;
    }

    public HotelArea getArea() {
        return area;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public AccessResult getResult() {
        return result;
    }

    public AccessDenialReason getDenialReason() {
        return denialReason;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }
}
