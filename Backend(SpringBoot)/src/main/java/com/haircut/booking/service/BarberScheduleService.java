package com.haircut.booking.service;

import com.haircut.booking.dto.BarberScheduleRequest;
import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.BarberSchedule;
import com.haircut.booking.repository.BarberRepository;
import com.haircut.booking.repository.BarberScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarberScheduleService {

    private final BarberScheduleRepository scheduleRepository;
    private final BarberRepository barberRepository;

    /** Lấy lịch làm việc của thợ trong khoảng ngày */
    public List<BarberSchedule> getSchedules(Long barberId, LocalDate from, LocalDate to) {
        return scheduleRepository
                .findByBarberIdAndWorkDateBetweenOrderByWorkDateAsc(barberId, from, to);
    }

    /** Admin: thêm hoặc cập nhật lịch làm 1 ngày (upsert) */
    @Transactional
    public BarberSchedule setSchedule(Long barberId, BarberScheduleRequest req) {
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new BookingService.NotFoundException("Không tìm thấy thợ"));

        LocalDate date  = LocalDate.parse(req.getWorkDate());
        LocalTime start = LocalTime.parse(req.getStartTime());
        LocalTime end   = LocalTime.parse(req.getEndTime());

        if (!end.isAfter(start))
            throw new BookingService.BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");

        BarberSchedule schedule = scheduleRepository
                .findByBarberIdAndWorkDate(barberId, date)
                .orElse(BarberSchedule.builder().barber(barber).workDate(date).build());

        schedule.setStartTime(start);
        schedule.setEndTime(end);
        return scheduleRepository.save(schedule);
    }

    /** Admin: xóa lịch 1 ngày (ngày nghỉ) */
    @Transactional
    public void deleteSchedule(Long barberId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        BarberSchedule schedule = scheduleRepository
                .findByBarberIdAndWorkDate(barberId, date)
                .orElseThrow(() -> new BookingService.NotFoundException("Không có lịch ngày này"));
        scheduleRepository.delete(schedule);
    }

    /** Lấy lịch làm việc của thợ trong 1 ngày, trả về Optional */
    public Optional<BarberSchedule> getScheduleForDate(Long barberId, LocalDate date) {
        return scheduleRepository.findByBarberIdAndWorkDate(barberId, date);
    }

    /** Check thợ có làm việc vào thời điểm cụ thể không */
    public boolean isWorkingAt(Long barberId, LocalDate date, LocalTime time) {
        return scheduleRepository.findByBarberIdAndWorkDate(barberId, date)
                .map(s -> !time.isBefore(s.getStartTime()) && !time.isAfter(s.getEndTime()))
                .orElse(false);
    }
}