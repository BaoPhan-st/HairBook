package com.haircut.booking.controller;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

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
            this.totalUsers = totalUsers;
            this.totalCustomers = totalCustomers;
            this.totalAdmins = totalAdmins;
            this.totalBookings = totalBookings;
            this.pendingBookings = pendingBookings;
            this.confirmedBookings = confirmedBookings;
            this.completedBookings = completedBookings;
            this.cancelledBookings = cancelledBookings;
            this.todayBookings = todayBookings;
        }
    }

    @Data
    public static class RoleUpdateRequest {
        private String role;
    }

    @Data
    public static class StatusUpdateRequest {
        private String status;
    }

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

    // ─── 1. Dashboard tổng quan ───────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        long totalUsers     = userRepository.count();
        long totalCustomers = userRepository.countByRole(User.Role.CUSTOMER);
        long totalAdmins    = userRepository.countByRole(User.Role.ADMIN);

        long totalBookings     = bookingRepository.count();
        long pendingBookings   = countBookingsByStatus(Booking.Status.PENDING);
        long confirmedBookings = countBookingsByStatus(Booking.Status.CONFIRMED);
        long completedBookings = countBookingsByStatus(Booking.Status.COMPLETED);
        long cancelledBookings = countBookingsByStatus(Booking.Status.CANCELLED_BY_CUSTOMER)
                + countBookingsByStatus(Booking.Status.CANCELLED_BY_SALON);

        // Bookings hôm nay
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);
        long todayBookings = bookingRepository
                .findByBarberAndDate(null, startOfDay, endOfDay).size(); // fallback nếu không có query riêng
        // Dùng count trực tiếp từ tất cả bookings hôm nay
        todayBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingTime() != null
                        && !b.getBookingTime().isBefore(startOfDay)
                        && b.getBookingTime().isBefore(endOfDay))
                .count();

        return ResponseEntity.ok(new DashboardResponse(
                totalUsers, totalCustomers, totalAdmins,
                totalBookings, pendingBookings, confirmedBookings, completedBookings, cancelledBookings,
                todayBookings
        ));
    }

    private long countBookingsByStatus(Booking.Status status) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == status)
                .count();
    }

    // ─── 2. Danh sách tất cả users ────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ─── 3. Tìm kiếm user ────────────────────────────────────────────────────

    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam("q") String query) {
        List<Map<String, Object>> users = userRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ─── 4. Chi tiết 1 user ───────────────────────────────────────────────────

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok((Object) toUserMap(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy người dùng")));
    }

    // ─── 5. Cập nhật role ─────────────────────────────────────────────────────

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest req) {
        return userRepository.findById(id).map(user -> {
            try {
                User.Role newRole = User.Role.valueOf(req.getRole().toUpperCase());
                user.setRole(newRole);
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
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest req) {
        return userRepository.findById(id).map(user -> {
            try {
                User.Status newStatus = User.Status.valueOf(req.getStatus().toUpperCase());
                user.setStatus(newStatus);
                userRepository.save(user);
                return ResponseEntity.ok((Object) toUserMap(user));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body((Object) Map.of("error", "Status không hợp lệ. Dùng ACTIVE hoặc LOCKED"));
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Không tìm thấy người dùng")));
    }
}