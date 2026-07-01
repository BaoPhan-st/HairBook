package com.haircut.booking.controller;

import com.haircut.booking.dto.BarberScheduleRequest;
import com.haircut.booking.entity.BarberSchedule;
import com.haircut.booking.service.BarberScheduleService;
import com.haircut.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/barbers/{barberId}/schedules")
@RequiredArgsConstructor
public class BarberScheduleController {

    private final BarberScheduleService scheduleService;

    // GET /api/barbers/{id}/schedules?from=2026-07-01&to=2026-07-31
    @GetMapping
    public ResponseEntity<?> getSchedules(
            @PathVariable Long barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<BarberSchedule> list = scheduleService.getSchedules(barberId, from, to);
        List<Map<String, Object>> result = list.stream().map(s -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",        s.getId());
            m.put("workDate",  s.getWorkDate().toString());
            m.put("startTime", s.getStartTime().toString());
            m.put("endTime",   s.getEndTime().toString());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    // POST /api/barbers/{id}/schedules — thêm hoặc sửa lịch 1 ngày
    @PostMapping
    public ResponseEntity<?> setSchedule(
            @PathVariable Long barberId,
            @RequestBody BarberScheduleRequest req) {
        try {
            BarberSchedule saved = scheduleService.setSchedule(barberId, req);
            return ResponseEntity.ok(Map.of(
                    "id",        saved.getId(),
                    "workDate",  saved.getWorkDate().toString(),
                    "startTime", saved.getStartTime().toString(),
                    "endTime",   saved.getEndTime().toString()
            ));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (BookingService.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/barbers/{id}/schedules?date=2026-07-01
    @DeleteMapping
    public ResponseEntity<?> deleteSchedule(
            @PathVariable Long barberId,
            @RequestParam String date) {
        try {
            scheduleService.deleteSchedule(barberId, date);
            return ResponseEntity.ok(Map.of("message", "Đã xóa lịch ngày " + date));
        } catch (BookingService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}