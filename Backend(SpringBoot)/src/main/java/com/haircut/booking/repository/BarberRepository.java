package com.haircut.booking.repository;

import com.haircut.booking.entity.Barber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarberRepository extends JpaRepository<Barber, Long> {

    List<Barber> findByAvailableTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Barber b
            WHERE b.id = :id
            """)
    Optional<Barber> findByIdForUpdate(@Param("id") Long id);
}