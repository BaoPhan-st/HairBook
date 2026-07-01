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

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

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
}