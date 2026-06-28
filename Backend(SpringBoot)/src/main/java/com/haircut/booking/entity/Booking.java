package com.haircut.booking.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private HaircutService service;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "booking_end_time")
    private LocalDateTime bookingEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @PrePersist
    public void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = Status.PENDING;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean blocksTimeSlot() {
        return status == Status.PENDING
                || status == Status.CONFIRMED
                || status == Status.IN_PROGRESS;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED
                || status == Status.CANCELLED_BY_CUSTOMER
                || status == Status.CANCELLED_BY_SALON;
    }

    public boolean isCustomerModifiable() {
        return status == Status.PENDING || status == Status.CONFIRMED;
    }

    public enum Status {
        PENDING,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED_BY_CUSTOMER,
        CANCELLED_BY_SALON,
        NO_SHOW,
        CANCELLED
    }
}