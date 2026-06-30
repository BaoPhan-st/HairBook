package com.haircut.booking.controller;

import com.haircut.booking.entity.*;
import com.haircut.booking.repository.*;
import com.haircut.booking.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository    bookingRepository;
    private final UserService          userService;
    private final BarberRepository     barberRepository;
    private final ServiceRepository    serviceRepository;

    @Data
    public static class BookingRequest {
        private Long   barberId;
        private Long   serviceId;
        private String bookingTime;
        private String note;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        Barber barber = barberRepository.findById(req.getBarberId()).orElse(null);
        if (barber == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Thợ không tồn tại"));

        HaircutService service = serviceRepository.findById(req.getServiceId())
                .orElse(null);
        if (service == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Dịch vụ không tồn tại"));

        LocalDateTime bookingTime = LocalDateTime.parse(req.getBookingTime());

        List<Booking> conflict = bookingRepository.findByBarberAndDate(
                barber.getId(),
                bookingTime.minusMinutes(1),
                bookingTime.plusMinutes(1)
        );
        if (!conflict.isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Khung giờ này đã được đặt, vui lòng chọn giờ khác"));

        Booking booking = Booking.builder()
                .user(user)
                .barber(barber)
                .service(service)
                .bookingTime(bookingTime)
                .note(req.getNote())
                .build();

        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping("/my")
    public ResponseEntity<List<?>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Booking> bookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(bookings.stream().map(this::toDto).toList());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null)
            return ResponseEntity.notFound().build();

        if (!booking.getUser().getEmail().equals(userDetails.getUsername()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (booking.getStatus() == Booking.Status.CANCELLED)
            return ResponseEntity.badRequest().body(Map.of("error", "Lịch đã được huỷ trước đó"));

        booking.setStatus(Booking.Status.CANCELLED);
        return ResponseEntity.ok(toDto(bookingRepository.save(booking)));
    }

    private Map<String, Object> toDto(Booking b) {
        return Map.of(
                "id",          b.getId(),
                "barber",      b.getBarber() != null ? Map.of(
                        "id",       b.getBarber().getId(),
                        "name",     b.getBarber().getName(),
                        "specialty",b.getBarber().getSpecialty() != null ? b.getBarber().getSpecialty() : "",
                        "rating",   b.getBarber().getRating() != null ? b.getBarber().getRating() : 0.0,
                        "imageUrl", b.getBarber().getImageUrl() != null ? b.getBarber().getImageUrl() : ""
                ) : Map.of(),
                "service",     Map.of(
                        "id",       b.getService().getId(),
                        "name",     b.getService().getName(),
                        "price",    b.getService().getPrice() != null ? b.getService().getPrice() : 0.0,
                        "durationMinutes", b.getService().getDurationMinutes() != null ? b.getService().getDurationMinutes() : 0
                ),
                "bookingTime", b.getBookingTime().toString(),
                "status",      b.getStatus().name(),
                "note",        b.getNote() != null ? b.getNote() : "",
                "createdAt",   b.getCreatedAt().toString()
        );
    }
}