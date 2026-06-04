package com.haircut.booking.controller;

import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.repository.BarberRepository;
import com.haircut.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
public class BarberController {

    private final BarberRepository barberRepository;
    private final BookingRepository bookingRepository;

    @GetMapping
    public List<Barber> getAllBarbers() {
        return barberRepository.findAll();
    }

    @GetMapping("/{id}/available-slots")
    public ResponseEntity<List<String>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(LocalTime.MAX);

        List<Booking> booked = bookingRepository.findByBarberAndDate(id, start, end);

        List<String> allSlots = new ArrayList<>();
        LocalTime cursor = LocalTime.of(8, 0);
        while (!cursor.isAfter(LocalTime.of(19, 30))) {
            allSlots.add(String.format("%02d:%02d", cursor.getHour(), cursor.getMinute()));
            cursor = cursor.plusMinutes(30);
        }

        for (Booking b : booked) {
            String bookedSlot = String.format("%02d:%02d",
                    b.getBookingTime().getHour(), b.getBookingTime().getMinute());
            allSlots.remove(bookedSlot);
        }

        return ResponseEntity.ok(allSlots);
    }
}