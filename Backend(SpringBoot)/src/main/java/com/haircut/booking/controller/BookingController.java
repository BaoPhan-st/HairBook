package com.haircut.booking.controller;

import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.HaircutService;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BarberRepository;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.ServiceRepository;
import com.haircut.booking.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final int MAX_ADVANCE_BOOKING_DAYS = 30;
    private static final int SLOT_INTERVAL_MINUTES = 30;
    private static final int OPENING_HOUR = 8;
    private static final int CLOSING_HOUR = 20;
    private static final int CUSTOMER_CHANGE_DEADLINE_HOURS = 2;

    private static final List<Booking.Status> BLOCKING_STATUSES = List.of(
            Booking.Status.PENDING,
            Booking.Status.CONFIRMED,
            Booking.Status.IN_PROGRESS
    );

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final BarberRepository barberRepository;
    private final ServiceRepository serviceRepository;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingRequest {

        @NotNull(message = "barberId không được để trống")
        private Long barberId;

        @NotNull(message = "serviceId không được để trống")
        private Long serviceId;

        @NotBlank(message = "bookingTime không được để trống")
        private String bookingTime;

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelBookingRequest {

        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
        private String cancelReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RescheduleBookingRequest {

        @NotBlank(message = "bookingTime không được để trống")
        private String bookingTime;

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        private String note;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = getCurrentUser(userDetails);

        Barber barber = barberRepository.findByIdForUpdate(request.getBarberId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thợ cắt tóc"
                ));

        if (!Boolean.TRUE.equals(barber.getAvailable())) {
            return error(
                    HttpStatus.CONFLICT,
                    "Thợ này hiện không nhận lịch. Vui lòng chọn thợ khác."
            );
        }

        HaircutService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy dịch vụ"
                ));

        int durationMinutes = getValidatedServiceDuration(service);
        LocalDateTime bookingStartTime = parseBookingTime(request.getBookingTime());
        LocalDateTime bookingEndTime = bookingStartTime.plusMinutes(durationMinutes);

        String bookingTimeError = validateBookingTime(
                bookingStartTime,
                bookingEndTime
        );

        if (bookingTimeError != null) {
            return error(HttpStatus.BAD_REQUEST, bookingTimeError);
        }

        boolean hasConflict = !bookingRepository.findOverlappingBookings(
                barber.getId(),
                bookingStartTime,
                bookingEndTime,
                BLOCKING_STATUSES,
                null
        ).isEmpty();

        if (hasConflict) {
            return error(
                    HttpStatus.CONFLICT,
                    "Khung giờ này không còn trống. Vui lòng chọn giờ khác."
            );
        }

        Booking booking = Booking.builder()
                .user(currentUser)
                .barber(barber)
                .service(service)
                .bookingTime(bookingStartTime)
                .bookingEndTime(bookingEndTime)
                .note(normalizeText(request.getNote()))
                .status(Booking.Status.PENDING)
                .build();

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(savedBooking));
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = getCurrentUser(userDetails);

        List<Map<String, Object>> response = bookingRepository
                .findByUserOrderByBookingTimeDesc(currentUser)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = getCurrentUser(userDetails);

        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch hẹn"
                ));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            return error(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy lịch hẹn này."
            );
        }

        if (booking.getStatus() == Booking.Status.COMPLETED) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể hủy lịch đã hoàn thành."
            );
        }

        if (booking.getStatus() == Booking.Status.IN_PROGRESS) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể hủy lịch đang được thực hiện."
            );
        }

        if (booking.getStatus() == Booking.Status.NO_SHOW) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể hủy lịch đã được đánh dấu vắng mặt."
            );
        }

        if (booking.isCancelled()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Lịch hẹn này đã được hủy trước đó."
            );
        }

        if (!booking.isCustomerModifiable()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái lịch hẹn hiện tại không cho phép hủy."
            );
        }

        LocalDateTime changeDeadline = LocalDateTime.now()
                .plusHours(CUSTOMER_CHANGE_DEADLINE_HOURS);

        if (!booking.getBookingTime().isAfter(changeDeadline)) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Bạn chỉ có thể hủy lịch trước giờ hẹn ít nhất 2 giờ."
            );
        }

        String cancelReason = request == null
                ? null
                : normalizeText(request.getCancelReason());

        booking.setStatus(Booking.Status.CANCELLED_BY_CUSTOMER);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelReason(cancelReason);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        return ResponseEntity.ok(toDto(savedBooking));
    }

    @PutMapping("/{id}/reschedule")
    @Transactional
    public ResponseEntity<Map<String, Object>> rescheduleBooking(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = getCurrentUser(userDetails);

        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch hẹn"
                ));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            return error(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền đổi lịch hẹn này."
            );
        }

        if (booking.getStatus() == Booking.Status.COMPLETED) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể đổi lịch đã hoàn thành."
            );
        }

        if (booking.getStatus() == Booking.Status.IN_PROGRESS) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể đổi lịch đang được thực hiện."
            );
        }

        if (booking.getStatus() == Booking.Status.NO_SHOW) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể đổi lịch đã được đánh dấu vắng mặt."
            );
        }

        if (booking.isCancelled()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Không thể đổi lịch đã bị hủy."
            );
        }

        if (!booking.isCustomerModifiable()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái lịch hẹn hiện tại không cho phép đổi lịch."
            );
        }

        LocalDateTime changeDeadline = LocalDateTime.now()
                .plusHours(CUSTOMER_CHANGE_DEADLINE_HOURS);

        if (!booking.getBookingTime().isAfter(changeDeadline)) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Bạn chỉ có thể đổi lịch trước giờ hẹn ít nhất 2 giờ."
            );
        }

        Barber barber = barberRepository.findByIdForUpdate(
                        booking.getBarber().getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thợ cắt tóc"
                ));

        if (!Boolean.TRUE.equals(barber.getAvailable())) {
            return error(
                    HttpStatus.CONFLICT,
                    "Thợ này hiện không nhận lịch. Bạn chưa thể đổi lịch."
            );
        }

        HaircutService service = booking.getService();
        int durationMinutes = getValidatedServiceDuration(service);

        LocalDateTime newBookingStartTime = parseBookingTime(request.getBookingTime());
        LocalDateTime newBookingEndTime = newBookingStartTime.plusMinutes(durationMinutes);

        if (newBookingStartTime.equals(booking.getBookingTime())) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Giờ mới phải khác giờ hẹn hiện tại."
            );
        }

        String bookingTimeError = validateBookingTime(
                newBookingStartTime,
                newBookingEndTime
        );

        if (bookingTimeError != null) {
            return error(HttpStatus.BAD_REQUEST, bookingTimeError);
        }

        boolean hasConflict = !bookingRepository.findOverlappingBookings(
                barber.getId(),
                newBookingStartTime,
                newBookingEndTime,
                BLOCKING_STATUSES,
                booking.getId()
        ).isEmpty();

        if (hasConflict) {
            return error(
                    HttpStatus.CONFLICT,
                    "Khung giờ mới đã có người đặt. Vui lòng chọn giờ khác."
            );
        }

        booking.setBookingTime(newBookingStartTime);
        booking.setBookingEndTime(newBookingEndTime);
        booking.setStatus(Booking.Status.PENDING);
        booking.setCancelledAt(null);
        booking.setCancelReason(null);

        if (request.getNote() != null) {
            booking.setNote(normalizeText(request.getNote()));
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        return ResponseEntity.ok(toDto(savedBooking));
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null
                || userDetails.getUsername() == null
                || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn."
            );
        }

        try {
            return userService.findByEmail(userDetails.getUsername());
        } catch (UsernameNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy tài khoản tương ứng với phiên đăng nhập."
            );
        }
    }

    private int getValidatedServiceDuration(HaircutService service) {
        if (service == null
                || service.getDurationMinutes() == null
                || service.getDurationMinutes() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dịch vụ chưa có thời lượng hợp lệ."
            );
        }

        if (service.getDurationMinutes() > 480) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời lượng dịch vụ không được vượt quá 480 phút."
            );
        }

        return service.getDurationMinutes();
    }

    private LocalDateTime parseBookingTime(String bookingTimeValue) {
        if (bookingTimeValue == null || bookingTimeValue.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "bookingTime không được để trống."
            );
        }

        try {
            return LocalDateTime.parse(bookingTimeValue.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "bookingTime phải có định dạng ISO, ví dụ: 2026-07-01T10:00:00"
            );
        }
    }

    private String validateBookingTime(
            LocalDateTime bookingStartTime,
            LocalDateTime bookingEndTime
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate bookingDate = bookingStartTime.toLocalDate();
        LocalDate maximumBookingDate = now.toLocalDate()
                .plusDays(MAX_ADVANCE_BOOKING_DAYS);

        if (bookingDate.isBefore(now.toLocalDate())) {
            return "Không thể đặt lịch vào ngày trong quá khứ.";
        }

        if (bookingDate.isAfter(maximumBookingDate)) {
            return "Bạn chỉ có thể đặt lịch trong vòng 30 ngày tới.";
        }

        if (bookingStartTime.isBefore(now)) {
            return "Không thể đặt lịch vào thời gian đã qua.";
        }

        if (bookingStartTime.getMinute() % SLOT_INTERVAL_MINUTES != 0
                || bookingStartTime.getSecond() != 0
                || bookingStartTime.getNano() != 0) {
            return "Khung giờ đặt lịch phải theo bước 30 phút, ví dụ 08:00, 08:30, 09:00.";
        }

        LocalTime openingTime = LocalTime.of(OPENING_HOUR, 0);
        LocalTime closingTime = LocalTime.of(CLOSING_HOUR, 0);
        LocalTime requestedStartTime = bookingStartTime.toLocalTime();

        if (requestedStartTime.isBefore(openingTime)
                || !requestedStartTime.isBefore(closingTime)) {
            return "Salon chỉ nhận lịch từ 08:00 đến 20:00.";
        }

        LocalDateTime closingDateTime = LocalDateTime.of(
                bookingStartTime.toLocalDate(),
                closingTime
        );

        if (bookingEndTime.isAfter(closingDateTime)) {
            return "Dịch vụ kết thúc sau giờ đóng cửa. Vui lòng chọn giờ sớm hơn.";
        }

        return null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            return null;
        }

        return normalizedValue;
    }

    private boolean canCustomerModifyBooking(Booking booking) {
        if (booking == null || !booking.isCustomerModifiable()) {
            return false;
        }

        LocalDateTime deadline = LocalDateTime.now()
                .plusHours(CUSTOMER_CHANGE_DEADLINE_HOURS);

        return booking.getBookingTime().isAfter(deadline);
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status.value());

        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> toDto(Booking booking) {
        Map<String, Object> bookingDto = new LinkedHashMap<>();

        LocalDateTime bookingEndTime = booking.getBookingEndTime();

        if (bookingEndTime == null
                && booking.getBookingTime() != null
                && booking.getService() != null
                && booking.getService().getDurationMinutes() != null) {
            bookingEndTime = booking.getBookingTime()
                    .plusMinutes(booking.getService().getDurationMinutes());
        }

        bookingDto.put("id", booking.getId());
        bookingDto.put("barber", toBarberDto(booking.getBarber()));
        bookingDto.put("service", toServiceDto(booking.getService()));
        bookingDto.put(
                "bookingTime",
                booking.getBookingTime() == null
                        ? null
                        : booking.getBookingTime().toString()
        );
        bookingDto.put(
                "bookingEndTime",
                bookingEndTime == null
                        ? null
                        : bookingEndTime.toString()
        );
        bookingDto.put(
                "status",
                booking.getStatus() == null
                        ? Booking.Status.PENDING.name()
                        : booking.getStatus().name()
        );
        bookingDto.put("note", booking.getNote());
        bookingDto.put(
                "createdAt",
                booking.getCreatedAt() == null
                        ? null
                        : booking.getCreatedAt().toString()
        );
        bookingDto.put(
                "updatedAt",
                booking.getUpdatedAt() == null
                        ? null
                        : booking.getUpdatedAt().toString()
        );
        bookingDto.put(
                "cancelledAt",
                booking.getCancelledAt() == null
                        ? null
                        : booking.getCancelledAt().toString()
        );
        bookingDto.put("cancelReason", booking.getCancelReason());
        bookingDto.put(
                "canCancel",
                canCustomerModifyBooking(booking)
        );
        bookingDto.put(
                "canReschedule",
                canCustomerModifyBooking(booking)
        );

        return bookingDto;
    }

    private Map<String, Object> toBarberDto(Barber barber) {
        Map<String, Object> barberDto = new LinkedHashMap<>();

        if (barber == null) {
            return barberDto;
        }

        barberDto.put("id", barber.getId());
        barberDto.put("name", barber.getName());
        barberDto.put("specialty", barber.getSpecialty());
        barberDto.put("rating", barber.getRating());
        barberDto.put("imageUrl", barber.getImageUrl());
        barberDto.put("available", Boolean.TRUE.equals(barber.getAvailable()));

        return barberDto;
    }

    private Map<String, Object> toServiceDto(HaircutService service) {
        Map<String, Object> serviceDto = new LinkedHashMap<>();

        if (service == null) {
            return serviceDto;
        }

        serviceDto.put("id", service.getId());
        serviceDto.put("name", service.getName());
        serviceDto.put("description", service.getDescription());
        serviceDto.put("price", service.getPrice());
        serviceDto.put("durationMinutes", service.getDurationMinutes());
        serviceDto.put("imageUrl", service.getImageUrl());

        return serviceDto;
    }
}