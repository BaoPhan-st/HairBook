package com.haircut.booking.service;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    // ── Lấy danh sách booking cho admin ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Booking> getBookings(String statusStr, LocalDate date) {
        Booking.Status status = parseStatus(statusStr);

        if (status != null && date != null) {
            return bookingRepository.findByStatusAndDate(
                    status, date.atStartOfDay(), date.atTime(LocalTime.MAX));
        }
        if (status != null) {
            return bookingRepository.findByStatusOrderByBookingTimeAsc(status);
        }
        if (date != null) {
            return bookingRepository.findByDate(
                    date.atStartOfDay(), date.atTime(LocalTime.MAX));
        }
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    // ── Xác nhận booking: PENDING → CONFIRMED ────────────────────────────────

    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new BookingService.BadRequestException(
                    "Chỉ có thể xác nhận booking ở trạng thái PENDING. " +
                            "Trạng thái hiện tại: " + booking.getStatus().name());
        }

        booking.setStatus(Booking.Status.CONFIRMED);
        return bookingRepository.save(booking);
    }

    // ── Từ chối booking: PENDING → CANCELLED_BY_SALON ────────────────────────

    @Transactional
    public Booking rejectBooking(Long bookingId, String reason) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new BookingService.BadRequestException(
                    "Chỉ có thể từ chối booking ở trạng thái PENDING. " +
                            "Trạng thái hiện tại: " + booking.getStatus().name());
        }

        booking.setStatus(Booking.Status.CANCELLED_BY_SALON);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelReason(reason != null && !reason.isBlank()
                ? reason : "Salon không thể phục vụ trong khung giờ này");
        return bookingRepository.save(booking);
    }

    // ── Bắt đầu thực hiện: CONFIRMED → IN_PROGRESS ───────────────────────────

    @Transactional
    public Booking startBooking(Long bookingId) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new BookingService.BadRequestException(
                    "Chỉ có thể chuyển sang Đang thực hiện từ trạng thái CONFIRMED. " +
                            "Trạng thái hiện tại: " + booking.getStatus().name());
        }

        booking.setStatus(Booking.Status.IN_PROGRESS);
        return bookingRepository.save(booking);
    }

    // ── Hoàn thành booking: CONFIRMED hoặc IN_PROGRESS → COMPLETED ───────────

    @Transactional
    public Booking completeBooking(Long bookingId) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != Booking.Status.CONFIRMED
                && booking.getStatus() != Booking.Status.IN_PROGRESS) {
            throw new BookingService.BadRequestException(
                    "Chỉ có thể hoàn thành booking ở trạng thái CONFIRMED hoặc IN_PROGRESS. " +
                            "Trạng thái hiện tại: " + booking.getStatus().name());
        }

        booking.setStatus(Booking.Status.COMPLETED);
        return bookingRepository.save(booking);
    }

    // ── Đánh dấu khách không đến: CONFIRMED → NO_SHOW ────────────────────────

    @Transactional
    public Booking markNoShow(Long bookingId) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new BookingService.BadRequestException(
                    "Chỉ có thể đánh dấu NO_SHOW với booking đã CONFIRMED. " +
                            "Trạng thái hiện tại: " + booking.getStatus().name());
        }

        booking.setStatus(Booking.Status.NO_SHOW);
        return bookingRepository.save(booking);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingService.NotFoundException(
                        "Không tìm thấy booking id=" + id));
    }

    private Booking.Status parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return Booking.Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BookingService.BadRequestException(
                    "Status không hợp lệ: " + statusStr +
                            ". Giá trị hợp lệ: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, " +
                            "CANCELLED_BY_CUSTOMER, CANCELLED_BY_SALON, NO_SHOW");
        }
    }
}