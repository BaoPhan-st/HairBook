package com.haircut.booking.repository;

import com.haircut.booking.entity.HaircutService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<HaircutService, Long> {
}