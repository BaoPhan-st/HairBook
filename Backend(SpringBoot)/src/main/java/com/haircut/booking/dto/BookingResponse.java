package com.haircut.booking.dto;

import com.haircut.booking.entity.Booking;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {

    private Long id;
    private BarberInfo barber;
    private ServiceInfo service;
    private String bookingTime;
    private String bookingEndTime;
    private String status;
    private String note;
    private String createdAt;
    private String updatedAt;
    private String cancelledAt;
    private String cancelReason;

    @Data
    @Builder
    public static class BarberInfo {
        private Long id;
        private String name;
        private String specialty;
        private Double rating;
        private String imageUrl;
    }

    @Data
    @Builder
    public static class ServiceInfo {
        private Long id;
        private String name;
        private Double price;
        private Integer durationMinutes;
    }

    public static BookingResponse from(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .barber(b.getBarber() != null ? BarberInfo.builder()
                                                .id(b.getBarber().getId())
                                                .name(b.getBarber().getName())
                                                .specialty(b.getBarber().getSpecialty())
                                                .rating(b.getBarber().getRating())
                                                .imageUrl(b.getBarber().getImageUrl())
                                                .build() : null)
                .service(ServiceInfo.builder()
                        .id(b.getService().getId())
                        .name(b.getService().getName())
                        .price(b.getService().getPrice())
                        .durationMinutes(b.getService().getDurationMinutes())
                        .build())
                .bookingTime(b.getBookingTime() != null ? b.getBookingTime().toString() : null)
                .bookingEndTime(b.getBookingEndTime() != null ? b.getBookingEndTime().toString() : null)
                .status(b.getStatus().name())
                .note(b.getNote() != null ? b.getNote() : "")
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null)
                .updatedAt(b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : null)
                .cancelledAt(b.getCancelledAt() != null ? b.getCancelledAt().toString() : null)
                .cancelReason(b.getCancelReason())
                .build();
    }

}