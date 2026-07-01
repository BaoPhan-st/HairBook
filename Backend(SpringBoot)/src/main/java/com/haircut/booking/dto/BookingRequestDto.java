package com.haircut.booking.dto;

import lombok.Data;

@Data
public class BookingRequestDto {
    private Long barberId;
    private Long serviceId;
    private String bookingTime;
    private String note;
}