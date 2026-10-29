package com.hotelaria.models;

import com.hotelaria.models.enums.AreaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "hotel_areas")
public class HotelArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AreaStatus status = AreaStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected HotelArea() {
    }

    public HotelArea(String name, String description, String location,
                     LocalTime openingTime, LocalTime closingTime) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    /**
     * Indica se a área está aberta no horário informado. Áreas sem horário definido
     * funcionam 24h. Suporta horários que atravessam a meia-noite (ex.: 18:00 às 02:00).
     */
    public boolean isOpenAt(LocalTime time) {
        if (openingTime == null || closingTime == null) {
            return true;
        }
        if (openingTime.isBefore(closingTime)) {
            return !time.isBefore(openingTime) && time.isBefore(closingTime);
        }
        return !time.isBefore(openingTime) || time.isBefore(closingTime);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public AreaStatus getStatus() {
        return status;
    }

    public void setStatus(AreaStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
