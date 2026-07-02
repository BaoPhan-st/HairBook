package com.haircut.booking.repository;

import com.haircut.booking.entity.BarberSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BarberScheduleRepository extends JpaRepository<BarberSchedule, Long> {

    Optional<BarberSchedule> findByBarberIdAndWorkDate(Long barberId, LocalDate workDate);

    List<BarberSchedule> findByBarberIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long barberId, LocalDate from, LocalDate to);
}