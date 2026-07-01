package com.haircut.booking.controller;

import com.haircut.booking.dto.BookingRequestDto;
import com.haircut.booking.dto.BookingResponse;
import com.haircut.booking.dto.CancelRequest;
import com.haircut.booking.dto.RescheduleRequest;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.ReviewRepository;
import com.haircut.booking.service.BookingService;
import com.haircut.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final BookingService    bookingService;
    private final BookingRepository bookingRepository;
    private final UserService       userService;
    private final ReviewRepository  reviewRepository;

    // ── Tạo booking mới ──────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequestDto req,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
        }

        try {
            User user = userService.findByEmail(userDetails.getUsername());

            LocalDateTime bookingTime = parseDateTime(req.getBookingTime());
            if (bookingTime == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ngày giờ đặt lịch không hợp lệ"));
            }

            Booking saved = bookingService.createBooking(
                    user, req.getBarberId(), req.getServiceId(), bookingTime, req.getNote());

            return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(saved));

        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Đã xảy ra lỗi không mong muốn, vui lòng thử lại"));
        }
    }

    // ── Lịch sử của khách hàng hiện tại ─────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
        }
        User user = userService.findByEmail(userDetails.getUsername());
        List<Booking> bookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(bookings.stream()
                .map(b -> BookingResponse.from(b, reviewRepository.findByBookingId(b.getId()).orElse(null)))
                .toList());
    }

    // ── Hủy booking ───────────────────────────────────────────────────────────

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
        }
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            String reason = req != null ? req.getReason() : null;
            Booking cancelled = bookingService.cancelBooking(user, id, reason);
            return ResponseEntity.ok(BookingResponse.from(cancelled));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Đổi lịch (reschedule) ────────────────────────────────────────────────

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable Long id,
            @RequestBody RescheduleRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
        }
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            LocalDateTime newTime = parseDateTime(req.getNewBookingTime());
            if (newTime == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ngày giờ mới không hợp lệ"));
            }
            Booking updated = bookingService.rescheduleBooking(user, id, newTime);
            return ResponseEntity.ok(BookingResponse.from(updated));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (BookingService.ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}