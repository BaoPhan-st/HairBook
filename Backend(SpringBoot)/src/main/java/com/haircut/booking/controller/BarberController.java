package com.haircut.booking.controller;

import com.haircut.booking.dto.BarberRequest;
import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.service.BarberService;
import com.haircut.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
public class BarberController {

    private final BarberService     barberService;
    private final BookingRepository bookingRepository;
    private final BookingService    bookingService;

    // GET /api/barbers — danh sách tất cả thợ
    @GetMapping
    public ResponseEntity<List<Barber>> getAllBarbers() {
        return ResponseEntity.ok(barberService.getAll());
    }

    // GET /api/barbers/{id} — chi tiết một thợ
    @GetMapping("/{id}")
    public ResponseEntity<?> getBarber(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(barberService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/barbers — thêm thợ mới
    @PostMapping
    public ResponseEntity<?> createBarber(@RequestBody BarberRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tên thợ không được để trống"));
        Barber created = barberService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/barbers/{id} — sửa thông tin thợ
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBarber(
            @PathVariable Long id,
            @RequestBody BarberRequest req) {
        try {
            return ResponseEntity.ok(barberService.update(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/barbers/{id} — xóa thợ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBarber(@PathVariable Long id) {
        try {
            barberService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa thợ id=" + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/barbers/{id}/available-slots?date=&serviceId= — slot trống theo đúng thời lượng dịch vụ
    @GetMapping("/{id}/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long serviceId) {

        try {
            List<String> slots = bookingService.getAvailableSlots(id, date, serviceId);
            return ResponseEntity.ok(slots);
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/barbers/{id}/bookings?date= — lịch booking của thợ theo ngày
    @GetMapping("/{id}/bookings")
    public ResponseEntity<?> getBarberBookings(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            barberService.getById(id); // kiểm tra thợ tồn tại
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByBarberAndDate(id, start, end);

        List<Map<String, Object>> result = bookings.stream().map(b -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("bookingId",   b.getId());
            map.put("bookingTime", b.getBookingTime().toString());
            map.put("bookingEndTime", b.getBookingEndTime().toString());
            map.put("status",      b.getStatus().name());
            map.put("note",        b.getNote() != null ? b.getNote() : "");
            map.put("customer", Map.of(
                    "id",       b.getUser().getId(),
                    "fullName", b.getUser().getFullName(),
                    "phone",    b.getUser().getPhone() != null ? b.getUser().getPhone() : ""
            ));
            map.put("service", Map.of(
                    "id",    b.getService().getId(),
                    "name",  b.getService().getName(),
                    "price", b.getService().getPrice() != null ? b.getService().getPrice() : 0.0
            ));
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }
}