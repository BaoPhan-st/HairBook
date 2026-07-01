package com.haircut.booking.controller;

import com.haircut.booking.dto.BookingResponse;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.UserRepository;
import com.haircut.booking.service.AdminBookingService;
import com.haircut.booking.service.BookingService;
import com.haircut.booking.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository       userRepository;
    private final BookingRepository    bookingRepository;
    private final UserService          userService;
    private final AdminBookingService  adminBookingService;

    // ─── Inner DTOs ───────────────────────────────────────────────────────────

    @Data
    public static class DashboardResponse {
        private long totalUsers;
        private long totalCustomers;
        private long totalAdmins;
        private long totalBookings;
        private long pendingBookings;
        private long confirmedBookings;
        private long completedBookings;
        private long cancelledBookings;
        private long todayBookings;

        public DashboardResponse(long totalUsers, long totalCustomers, long totalAdmins,
                                 long totalBookings, long pendingBookings, long confirmedBookings,
                                 long completedBookings, long cancelledBookings, long todayBookings) {
            this.totalUsers        = totalUsers;
            this.totalCustomers    = totalCustomers;
            this.totalAdmins       = totalAdmins;
            this.totalBookings     = totalBookings;
            this.pendingBookings   = pendingBookings;
            this.confirmedBookings = confirmedBookings;
            this.completedBookings = completedBookings;
            this.cancelledBookings = cancelledBookings;
            this.todayBookings     = todayBookings;
        }
    }

    @Data
    public static class RoleUpdateRequest { private String role; }

    @Data
    public static class StatusUpdateRequest { private String status; }

    @Data
    public static class RejectRequest { private String reason; }

    // ─── Helper: map User entity → response Map ───────────────────────────────

    private Map<String, Object> toUserMap(User u) {
        return Map.of(
                "id",        u.getId(),
                "email",     u.getEmail(),
                "fullName",  u.getFullName(),
                "phone",     u.getPhone() != null ? u.getPhone() : "",
                "role",      u.getRole().name(),
                "status",    u.getStatus().name(),
                "createdAt", u.getCreatedAt() != null
                        ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : ""
        );
    }

    // ─── Kiểm tra quyền admin ─────────────────────────────────────────────────

    private User requireAdmin(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BookingService.ForbiddenException("Chưa đăng nhập");
        }
        User user = userService.findByEmail(userDetails.getUsername());
        if (user.getRole() != User.Role.ADMIN) {
            throw new BookingService.ForbiddenException("Chỉ Admin mới có quyền thực hiện thao tác này");
        }
        return user;
    }

    // ─── 1. Dashboard tổng quan ───────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }

        long totalUsers     = userRepository.count();
        long totalCustomers = userRepository.countByRole(User.Role.CUSTOMER);
        long totalAdmins    = userRepository.countByRole(User.Role.ADMIN);
        long totalBookings  = bookingRepository.count();

        // Dùng countByStatus thay vì findAll().stream().filter() — hiệu năng tốt hơn nhiều
        long pendingBookings   = bookingRepository.countByStatus(Booking.Status.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(Booking.Status.CONFIRMED);
        long completedBookings = bookingRepository.countByStatus(Booking.Status.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(Booking.Status.CANCELLED_BY_CUSTOMER)
                + bookingRepository.countByStatus(Booking.Status.CANCELLED_BY_SALON);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);
        long todayBookings = bookingRepository.findByDate(startOfDay, endOfDay).size();

        return ResponseEntity.ok(new DashboardResponse(
                totalUsers, totalCustomers, totalAdmins,
                totalBookings, pendingBookings, confirmedBookings, completedBookings,
                cancelledBookings, todayBookings
        ));
    }

    // ─── 2. Danh sách tất cả users ────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal UserDetails userDetails) {
        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        List<Map<String, Object>> users = userRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toUserMap).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ─── 3. Tìm kiếm user ────────────────────────────────────────────────────

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam("q") String query,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        List<Map<String, Object>> users = userRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream().map(this::toUserMap).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ─── 4. Chi tiết 1 user ───────────────────────────────────────────────────

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok((Object) toUserMap(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy người dùng")));
    }

    // ─── 5. Cập nhật role ─────────────────────────────────────────────────────

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                        @RequestBody RoleUpdateRequest req,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        return userRepository.findById(id).map(user -> {
            try {
                user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
                userRepository.save(user);
                return ResponseEntity.ok((Object) toUserMap(user));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body((Object) Map.of("error", "Role không hợp lệ. Dùng ADMIN hoặc CUSTOMER"));
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Không tìm thấy người dùng")));
    }

    // ─── 6. Khoá / Mở khoá tài khoản ─────────────────────────────────────────

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody StatusUpdateRequest req,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        return userRepository.findById(id).map(user -> {
            try {
                user.setStatus(User.Status.valueOf(req.getStatus().toUpperCase()));
                userRepository.save(user);
                return ResponseEntity.ok((Object) toUserMap(user));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body((Object) Map.of("error", "Status không hợp lệ. Dùng ACTIVE hoặc LOCKED"));
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Không tìm thấy người dùng")));
    }

    /**
     * Xem tất cả booking. Tham số status và date đều tùy chọn.
     */
    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {

        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        try {
            List<Booking> bookings = adminBookingService.getBookings(status, date);
            return ResponseEntity.ok(bookings.stream().map(BookingResponse::from).toList());
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin xác nhận booking: PENDING → CONFIRMED.
     */
    @PutMapping("/bookings/{id}/confirm")
    public ResponseEntity<?> confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        try {
            Booking confirmed = adminBookingService.confirmBooking(id);
            return ResponseEntity.ok(BookingResponse.from(confirmed));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin từ chối booking: PENDING → CANCELLED_BY_SALON.
     */
    @PutMapping("/bookings/{id}/reject")
    public ResponseEntity<?> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) RejectRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        try {
            String reason = req != null ? req.getReason() : null;
            Booking rejected = adminBookingService.rejectBooking(id, reason);
            return ResponseEntity.ok(BookingResponse.from(rejected));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin đánh dấu booking hoàn thành: CONFIRMED / IN_PROGRESS → COMPLETED.
     */
    @PutMapping("/bookings/{id}/complete")
    public ResponseEntity<?> completeBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        try {
            Booking completed = adminBookingService.completeBooking(id);
            return ResponseEntity.ok(BookingResponse.from(completed));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin đánh dấu khách không đến: CONFIRMED → NO_SHOW.
     */
    @PutMapping("/bookings/{id}/no-show")
    public ResponseEntity<?> markNoShow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try { requireAdmin(userDetails); } catch (BookingService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        try {
            Booking noShow = adminBookingService.markNoShow(id);
            return ResponseEntity.ok(BookingResponse.from(noShow));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}