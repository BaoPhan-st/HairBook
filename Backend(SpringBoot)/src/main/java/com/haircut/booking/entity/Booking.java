package com.haircut.booking.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private HaircutService service;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "booking_end_time", nullable = false)
    private LocalDateTime bookingEndTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String note;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime cancelledAt;

    @Column(length = 255)
    private String cancelReason;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED_BY_CUSTOMER,
        CANCELLED_BY_SALON,
        NO_SHOW;

        /** Các trạng thái coi là "đã hủy" — không tính vào việc kiểm tra trùng lịch. */
        public boolean isCancelled() {
            return this == CANCELLED_BY_CUSTOMER || this == CANCELLED_BY_SALON;
        }

        /** Các trạng thái không cho phép hủy / đổi lịch nữa. */
        public boolean isFinal() {
            return this == COMPLETED || this == CANCELLED_BY_CUSTOMER
                    || this == CANCELLED_BY_SALON || this == NO_SHOW;
        }
    }
}