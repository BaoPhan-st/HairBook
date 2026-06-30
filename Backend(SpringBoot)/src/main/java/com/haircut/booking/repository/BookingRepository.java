package com.haircut.booking.repository;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.barber.id = :barberId
          AND b.bookingTime >= :start
          AND b.bookingTime < :end
          AND b.status NOT IN ('CANCELLED')
    """)
    List<Booking> findByBarberAndDate(
            @Param("barberId") Long barberId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end
    );
}