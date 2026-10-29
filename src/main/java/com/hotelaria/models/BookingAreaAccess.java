package com.hotelaria.models;

import com.hotelaria.models.enums.AccessGrantStatus;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Autorização de acesso de uma hospedagem a uma área do hotel.
 * A janela é semiaberta: [validFrom, validUntil).
 */
@Entity
@Table(name = "booking_area_access",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_area_access", columnNames = {"booking_id", "area_id"}))
public class BookingAreaAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private HotelArea area;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessGrantStatus status = AccessGrantStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by_user_id", nullable = false)
    private User grantedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private User revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BookingAreaAccess() {
    }

    public BookingAreaAccess(Booking booking, HotelArea area, LocalDateTime validFrom,
                             LocalDateTime validUntil, User grantedBy) {
        this.booking = booking;
        this.area = area;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.grantedBy = grantedBy;
    }

    public boolean isWithinWindow(LocalDateTime moment) {
        return !moment.isBefore(validFrom) && moment.isBefore(validUntil);
    }

    /** Reativa uma autorização revogada com uma nova janela. */
    public void reactivate(LocalDateTime newValidFrom, LocalDateTime newValidUntil, User newGrantedBy) {
        this.validFrom = newValidFrom;
        this.validUntil = newValidUntil;
        this.grantedBy = newGrantedBy;
        this.status = AccessGrantStatus.ACTIVE;
        this.revokedBy = null;
        this.revokedAt = null;
    }

    public void revoke(User user, LocalDateTime moment) {
        this.status = AccessGrantStatus.REVOKED;
        this.revokedBy = user;
        this.revokedAt = moment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public HotelArea getArea() {
        return area;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public AccessGrantStatus getStatus() {
        return status;
    }

    public void setStatus(AccessGrantStatus status) {
        this.status = status;
    }

    public User getGrantedBy() {
        return grantedBy;
    }

    public User getRevokedBy() {
        return revokedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
