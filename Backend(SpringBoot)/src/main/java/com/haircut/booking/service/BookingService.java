package com.haircut.booking.service;

import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.HaircutService;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BarberRepository;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BarberRepository barberRepository;
    private final ServiceRepository serviceRepository;
    private final BarberScheduleService barberScheduleService;

    /** Giờ mở cửa / đóng cửa salon dùng để sinh slot. */
    public static final LocalTime OPEN_TIME  = LocalTime.of(8, 0);
    public static final LocalTime CLOSE_TIME = LocalTime.of(20, 0);
    public static final int SLOT_STEP_MINUTES = 30;

    /** Chỉ cho đặt lịch trong vòng 30 ngày tới. */
    public static final int MAX_DAYS_AHEAD = 30;

    /** Không cho hủy lịch nếu còn dưới 2 giờ nữa là tới giờ hẹn. */
    public static final int CANCEL_CUTOFF_HOURS = 2;

    /** Custom exceptions (mapped to HTTP status ở controller) */

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    /** Tạo booking mới */

    @Transactional
    public Booking createBooking(User user, Long barberId, Long serviceId,
                                 LocalDateTime startTime, String note) {

        if (barberId == null) throw new BadRequestException("Vui lòng chọn thợ cắt tóc");
        if (serviceId == null) throw new BadRequestException("Vui lòng chọn dịch vụ");
        if (startTime == null) throw new BadRequestException("Vui lòng chọn ngày giờ đặt lịch");

        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new NotFoundException("Thợ không tồn tại"));
        if (Boolean.FALSE.equals(barber.getAvailable())) {
            throw new BadRequestException("Thợ này hiện không nhận lịch");
        }

        HaircutService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Dịch vụ không tồn tại"));

        validateBookingTime(startTime);
     // Check thợ có lịch làm ngày này không
        if (!barberScheduleService.isWorkingAt(barberId, startTime.toLocalDate(), startTime.toLocalTime())) {
            throw new BadRequestException("Thợ không làm việc vào khung giờ này");
        }

        LocalDateTime endTime = startTime.plusMinutes(durationOf(service));

        ensureNoOverlap(barberId, startTime, endTime, null);

        if (note != null && note.length() > 500) {
            note = note.substring(0, 500);
        }

        Booking booking = Booking.builder()
                .user(user)
                .barber(barber)
                .service(service)
                .bookingTime(startTime)
                .bookingEndTime(endTime)
                .note(note)
                .status(Booking.Status.PENDING)
                .build();

        return bookingRepository.save(booking);
    }

    /** Hủy booking */

    @Transactional
    public Booking cancelBooking(User user, Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch hẹn"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền hủy lịch hẹn này");
        }

        if (booking.getStatus().isCancelled()) {
            throw new BadRequestException("Lịch hẹn đã được hủy trước đó");
        }
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new BadRequestException("Không thể hủy lịch hẹn đã hoàn thành");
        }
        if (booking.getStatus() == Booking.Status.NO_SHOW) {
            throw new BadRequestException("Không thể hủy lịch hẹn này");
        }

        long minutesUntil = java.time.Duration.between(LocalDateTime.now(), booking.getBookingTime()).toMinutes();
        if (minutesUntil < CANCEL_CUTOFF_HOURS * 60) {
            throw new BadRequestException(
                    "Không thể hủy lịch trong vòng " + CANCEL_CUTOFF_HOURS + " giờ trước giờ hẹn");
        }

        booking.setStatus(Booking.Status.CANCELLED_BY_CUSTOMER);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelReason(reason != null && !reason.isBlank() ? reason : "Khách hàng tự hủy");

        return bookingRepository.save(booking);
    }

    /** Đổi lịch */

    @Transactional
    public Booking rescheduleBooking(User user, Long bookingId, LocalDateTime newStartTime) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch hẹn"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền đổi lịch hẹn này");
        }

        if (booking.getStatus().isFinal()) {
            throw new BadRequestException("Không thể đổi lịch hẹn ở trạng thái hiện tại");
        }

        if (newStartTime == null) {
            throw new BadRequestException("Vui lòng chọn ngày giờ mới");
        }

        validateBookingTime(newStartTime);
     // Check thợ có lịch làm ngày mới không
        if (!barberScheduleService.isWorkingAt(
                booking.getBarber().getId(),
                newStartTime.toLocalDate(),
                newStartTime.toLocalTime())) {
            throw new BadRequestException("Thợ không làm việc vào khung giờ này");
        }

        int duration = durationOf(booking.getService());
        LocalDateTime newEndTime = newStartTime.plusMinutes(duration);

        ensureNoOverlap(booking.getBarber().getId(), newStartTime, newEndTime, booking.getId());

        booking.setBookingTime(newStartTime);
        booking.setBookingEndTime(newEndTime);
        // Đổi lịch thì cần salon xác nhận lại từ đầu
        booking.setStatus(Booking.Status.PENDING);

        return bookingRepository.save(booking);
    }

    /** Lấy slot trống */
    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(Long barberId, LocalDate date, Long serviceId) {
        barberRepository.findById(barberId)
                .orElseThrow(() -> new NotFoundException("Thợ không tồn tại"));

        // Lấy lịch làm việc của thợ ngày đó
        var scheduleOpt = barberScheduleService.getScheduleForDate(barberId, date);
        if (scheduleOpt.isEmpty()) {
            return java.util.List.of(); // thợ không làm ngày này
        }
        LocalTime openTime  = scheduleOpt.get().getStartTime();
        LocalTime closeTime = scheduleOpt.get().getEndTime();

        int duration = 30;
        if (serviceId != null) {
            HaircutService service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NotFoundException("Dịch vụ không tồn tại"));
            duration = durationOf(service);
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        List<Booking> existing = bookingRepository.findByBarberAndDate(barberId, dayStart, dayEnd);

        LocalDateTime now = LocalDateTime.now();
        boolean isToday = date.isEqual(LocalDate.now());

        java.util.List<String> result = new java.util.ArrayList<>();
        LocalTime cursor = openTime;

        while (true) {
            LocalDateTime slotStart = date.atTime(cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(duration);

            // Slot phải kết thúc trước hoặc đúng giờ đóng cửa của thợ
            if (slotEnd.toLocalTime().isAfter(closeTime)) break;
            if (slotEnd.isAfter(date.atTime(closeTime))) break;

            boolean isPast = isToday && !slotStart.isAfter(now);
            boolean overlaps = existing.stream().anyMatch(b ->
                    b.getBookingTime().isBefore(slotEnd) && b.getBookingEndTime().isAfter(slotStart));

            if (!isPast && !overlaps) {
                result.add(String.format("%02d:%02d", cursor.getHour(), cursor.getMinute()));
            }

            LocalTime next = cursor.plusMinutes(SLOT_STEP_MINUTES);
            if (next.isBefore(cursor)) break; // tràn qua nửa đêm
            cursor = next;
        }

        return result;
    }

    /** Helper */

    private void ensureNoOverlap(Long barberId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        List<Booking> overlapping = bookingRepository.findOverlappingForUpdate(barberId, start, end, excludeBookingId);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Khung giờ này đã có người đặt, vui lòng chọn giờ khác");
        }
    }

    private void validateBookingTime(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            throw new BadRequestException("Không thể đặt lịch ở thời điểm trong quá khứ");
        }
        if (startTime.toLocalDate().isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new BadRequestException("Chỉ có thể đặt lịch trong vòng " + MAX_DAYS_AHEAD + " ngày tới");
        }
        LocalTime t = startTime.toLocalTime();
        if (t.isBefore(OPEN_TIME) || t.isAfter(CLOSE_TIME)) {
            throw new BadRequestException("Giờ đặt lịch phải nằm trong khung giờ làm việc "
                    + OPEN_TIME + " - " + CLOSE_TIME);
        }
    }

    private int durationOf(HaircutService service) {
        return service.getDurationMinutes() != null && service.getDurationMinutes() > 0
                ? service.getDurationMinutes()
                : 30;
    }
}