package com.haircut.booking.repository;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"barber", "service"})
    List<Booking> findByUserOrderByBookingTimeDesc(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.barber.id = :barberId
              AND b.bookingTime < :candidateEndTime
              AND b.bookingEndTime > :candidateStartTime
              AND b.status IN :blockingStatuses
              AND (:excludedBookingId IS NULL OR b.id <> :excludedBookingId)
            """)
    List<Booking> findOverlappingBookings(
            @Param("barberId") Long barberId,
            @Param("candidateStartTime") LocalDateTime candidateStartTime,
            @Param("candidateEndTime") LocalDateTime candidateEndTime,
            @Param("blockingStatuses") List<Booking.Status> blockingStatuses,
            @Param("excludedBookingId") Long excludedBookingId
    );

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.barber.id = :barberId
              AND b.bookingTime >= :start
              AND b.bookingTime < :end
              AND b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')
            """)
    List<Booking> findByBarberAndDate(
            @Param("barberId") Long barberId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}