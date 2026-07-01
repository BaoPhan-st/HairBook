package com.haircut.booking.repository;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── Customer ──────────────────────────────────────────────────────────────

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    // ── Barber schedule ───────────────────────────────────────────────────────

    @Query("""
        SELECT b FROM Booking b
        WHERE b.barber.id = :barberId
          AND b.bookingTime < :end
          AND b.bookingEndTime > :start
          AND b.status NOT IN ('CANCELLED_BY_CUSTOMER', 'CANCELLED_BY_SALON')
        ORDER BY b.bookingTime ASC
    """)
    List<Booking> findByBarberAndDate(
            @Param("barberId") Long barberId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end
    );

    // ── Overlap check ────────────────────────────────────────────────────────

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.barber.id = :barberId
          AND b.status NOT IN ('CANCELLED_BY_CUSTOMER', 'CANCELLED_BY_SALON')
          AND b.bookingTime < :newEnd
          AND b.bookingEndTime > :newStart
          AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)
    """)
    List<Booking> findOverlappingForUpdate(
            @Param("barberId")         Long barberId,
            @Param("newStart")         LocalDateTime newStart,
            @Param("newEnd")           LocalDateTime newEnd,
            @Param("excludeBookingId") Long excludeBookingId
    );

    // ── Admin: đếm theo status ───────────────────────────────────────────────

    long countByStatus(Booking.Status status);

    // ── Admin: tất cả booking sắp xếp mới nhất trước ─────────────────────────

    List<Booking> findAllByOrderByCreatedAtDesc();

    // ── Admin: filter theo status ─────────────────────────────────────────────

    List<Booking> findByStatusOrderByBookingTimeAsc(Booking.Status status);

    // ── Admin: filter theo ngày (tất cả thợ) ─────────────────────────────────

    @Query("""
        SELECT b FROM Booking b
        WHERE b.bookingTime >= :start
          AND b.bookingTime < :end
        ORDER BY b.bookingTime ASC
    """)
    List<Booking> findByDate(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end
    );

    // ── Admin: filter theo status + ngày ─────────────────────────────────────

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = :status
          AND b.bookingTime >= :start
          AND b.bookingTime < :end
        ORDER BY b.bookingTime ASC
    """)
    List<Booking> findByStatusAndDate(
            @Param("status") Booking.Status status,
            @Param("start")  LocalDateTime start,
            @Param("end")    LocalDateTime end
    );
}