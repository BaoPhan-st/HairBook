package com.haircut.booking.dto;

import lombok.Data;

@Data
public class BarberScheduleRequest {
    private String workDate;   // "2026-07-01"
    private String startTime;  // "08:00"
    private String endTime;    // "17:00"
}