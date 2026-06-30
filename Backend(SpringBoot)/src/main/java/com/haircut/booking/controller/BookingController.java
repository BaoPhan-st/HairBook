package com.haircut.booking.controller;

import com.haircut.booking.dto.BookingRequestDto;
import com.haircut.booking.dto.BookingResponse;
import com.haircut.booking.dto.CancelRequest;
import com.haircut.booking.dto.RescheduleRequest;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.service.BookingService;
import com.haircut.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final BookingService bookingService;

    // POST /api/bookings — tạo lịch hẹn mới
    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequestDto req,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để đặt lịch"));
        }

        User user = userService.findByEmail(userDetails.getUsername());

        LocalDateTime bookingTime;
        try {
            bookingTime = LocalDateTime.parse(req.getBookingTime());
        } catch (DateTimeParseException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Định dạng ngày giờ không hợp lệ"));
        }

        try {
            Booking saved = bookingService.createBooking(
                    user, req.getBarberId(), req.getServiceId(), bookingTime, req.getNote());
            return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(saved));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/bookings/my — lịch sử đặt lịch của khách hiện tại
    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập"));
        }
        User user = userService.findByEmail(userDetails.getUsername());
        List<Booking> bookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(bookings.stream().map(BookingResponse::from).toList());
    }

    // PUT /api/bookings/{id}/cancel — hủy lịch hẹn
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập"));
        }
        User user = userService.findByEmail(userDetails.getUsername());
        String reason = req != null ? req.getReason() : null;

        try {
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

    // PUT /api/bookings/{id}/reschedule — đổi lịch hẹn
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable Long id,
            @RequestBody RescheduleRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập"));
        }
        User user = userService.findByEmail(userDetails.getUsername());

        LocalDateTime newTime;
        try {
            newTime = LocalDateTime.parse(req.getNewBookingTime());
        } catch (DateTimeParseException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Định dạng ngày giờ không hợp lệ"));
        }

        try {
            Booking rescheduled = bookingService.rescheduleBooking(user, id, newTime);
            return ResponseEntity.ok(BookingResponse.from(rescheduled));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (BookingService.ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}