package com.haircut.booking.controller;

import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.HaircutService;
import com.haircut.booking.repository.BarberRepository;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
public class BarberController {

    private static final int MAX_ADVANCE_BOOKING_DAYS = 30;
    private static final int SLOT_INTERVAL_MINUTES = 30;
    private static final int OPENING_HOUR = 8;
    private static final int CLOSING_HOUR = 20;
    private static final int MAX_SERVICE_DURATION_MINUTES = 480;

    private final BarberRepository barberRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<BarberResponse>> getAllBarbers() {
        List<BarberResponse> response = barberRepository.findByAvailableTrue()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Barber::getName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                )
                .map(this::toBarberResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/available-slots")
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam(name = "date", required = false) String dateValue,
            @RequestParam(name = "serviceId", required = false) Long serviceId
    ) {
        LocalDate selectedDate = parseDate(dateValue);

        if (serviceId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "serviceId không được để trống khi lấy khung giờ trống."
            );
        }

        validateSelectedDate(selectedDate);

        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thợ cắt tóc."
                ));

        if (!Boolean.TRUE.equals(barber.getAvailable())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thợ này hiện không nhận lịch. Vui lòng chọn thợ khác."
            );
        }

        HaircutService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy dịch vụ."
                ));

        int durationMinutes = getValidatedDurationMinutes(service);

        LocalDateTime startOfDay = selectedDate.atStartOfDay();
        LocalDateTime startOfNextDay = selectedDate.plusDays(1).atStartOfDay();

        List<Booking> bookedBookings = bookingRepository.findByBarberAndDate(
                barber.getId(),
                startOfDay,
                startOfNextDay
        );

        List<String> availableSlots = generateAvailableSlots(
                selectedDate,
                durationMinutes,
                bookedBookings
        );

        return ResponseEntity.ok(availableSlots);
    }

    private List<String> generateAvailableSlots(
            LocalDate selectedDate,
            int durationMinutes,
            List<Booking> bookedBookings
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime openingTime = LocalTime.of(OPENING_HOUR, 0);
        LocalTime closingTime = LocalTime.of(CLOSING_HOUR, 0);

        LocalTime currentSlot = openingTime;

        java.util.ArrayList<String> availableSlots = new java.util.ArrayList<>();

        while (!currentSlot.plusMinutes(durationMinutes).isAfter(closingTime)) {
            LocalDateTime candidateStartTime = LocalDateTime.of(
                    selectedDate,
                    currentSlot
            );

            LocalDateTime candidateEndTime = candidateStartTime
                    .plusMinutes(durationMinutes);

            boolean isPastTimeToday = selectedDate.equals(now.toLocalDate())
                    && !candidateStartTime.isAfter(now);

            boolean overlapsExistingBooking = bookedBookings.stream()
                    .anyMatch(booking -> overlaps(
                            candidateStartTime,
                            candidateEndTime,
                            booking
                    ));

            if (!isPastTimeToday && !overlapsExistingBooking) {
                availableSlots.add(
                        String.format(
                                "%02d:%02d",
                                currentSlot.getHour(),
                                currentSlot.getMinute()
                        )
                );
            }

            currentSlot = currentSlot.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return availableSlots;
    }

    private boolean overlaps(
            LocalDateTime candidateStartTime,
            LocalDateTime candidateEndTime,
            Booking existingBooking
    ) {
        if (existingBooking == null || !existingBooking.blocksTimeSlot()) {
            return false;
        }

        LocalDateTime existingStartTime = existingBooking.getBookingTime();

        if (existingStartTime == null) {
            return false;
        }

        LocalDateTime existingEndTime = getEffectiveBookingEndTime(existingBooking);

        return existingStartTime.isBefore(candidateEndTime)
                && existingEndTime.isAfter(candidateStartTime);
    }

    private LocalDateTime getEffectiveBookingEndTime(Booking booking) {
        if (booking.getBookingEndTime() != null) {
            return booking.getBookingEndTime();
        }

        HaircutService bookingService = booking.getService();

        if (bookingService != null
                && bookingService.getDurationMinutes() != null
                && bookingService.getDurationMinutes() > 0) {
            return booking.getBookingTime()
                    .plusMinutes(bookingService.getDurationMinutes());
        }

        return booking.getBookingTime()
                .plusMinutes(SLOT_INTERVAL_MINUTES);
    }

    private LocalDate parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "date không được để trống. Ví dụ hợp lệ: 2026-07-01"
            );
        }

        try {
            return LocalDate.parse(dateValue.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "date phải có định dạng yyyy-MM-dd. Ví dụ hợp lệ: 2026-07-01"
            );
        }
    }

    private void validateSelectedDate(LocalDate selectedDate) {
        LocalDate today = LocalDate.now();
        LocalDate maximumBookingDate = today.plusDays(MAX_ADVANCE_BOOKING_DAYS);

        if (selectedDate.isBefore(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể lấy giờ trống cho ngày trong quá khứ."
            );
        }

        if (selectedDate.isAfter(maximumBookingDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bạn chỉ có thể đặt lịch trong vòng 30 ngày tới."
            );
        }
    }

    private int getValidatedDurationMinutes(HaircutService service) {
        Integer durationMinutes = service.getDurationMinutes();

        if (durationMinutes == null || durationMinutes <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dịch vụ chưa có thời lượng hợp lệ."
            );
        }

        if (durationMinutes > MAX_SERVICE_DURATION_MINUTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời lượng dịch vụ không được vượt quá 480 phút."
            );
        }

        return durationMinutes;
    }

    private BarberResponse toBarberResponse(Barber barber) {
        return new BarberResponse(
                barber.getId(),
                barber.getName(),
                barber.getSpecialty(),
                barber.getImageUrl(),
                barber.getRating(),
                Boolean.TRUE.equals(barber.getAvailable())
        );
    }

    public record BarberResponse(
            Long id,
            String name,
            String specialty,
            String imageUrl,
            Double rating,
            Boolean available
    ) {
    }
}