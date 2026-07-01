package com.haircut.booking.dto;

import com.haircut.booking.entity.Booking;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {

    private Long id;
    private CustomerInfo customer;
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
    private ReviewInfo review;

    // ── Inner classes ──────────────────────────────────────────────────────────

    @Data @Builder
    public static class CustomerInfo {
        private Long id;
        private String fullName;
        private String phone;
        private String email;
    }

    @Data @Builder
    public static class BarberInfo {
        private Long id;
        private String name;
        private String specialty;
        private Double rating;
        private String imageUrl;
    }

    @Data @Builder
    public static class ServiceInfo {
        private Long id;
        private String name;
        private Double price;
        private Integer durationMinutes;
    }

    @Data @Builder
    public static class ReviewInfo {
        private int rating;
        private String comment;
    }

    // ── Static factory ─────────────────────────────────────────────────────────

    /** Map booking, không kèm review (dùng cho các màn hình không cần hiển thị đánh giá). */
    public static BookingResponse from(Booking b) {
        return from(b, null);
    }

    /** Map booking kèm review (nếu có) — dùng cho lịch sử khách hàng. */
    public static BookingResponse from(Booking b, com.haircut.booking.entity.Review review) {
        return BookingResponse.builder()
                .id(b.getId())
                // Customer
                .customer(b.getUser() != null ? CustomerInfo.builder()
                                                .id(b.getUser().getId())
                                                .fullName(b.getUser().getFullName())
                                                .phone(b.getUser().getPhone() != null ? b.getUser().getPhone() : "")
                                                .email(b.getUser().getEmail())
                                                .build() : null)
                // Barber
                .barber(b.getBarber() != null ? BarberInfo.builder()
                                                .id(b.getBarber().getId())
                                                .name(b.getBarber().getName())
                                                .specialty(b.getBarber().getSpecialty())
                                                .rating(b.getBarber().getRating())
                                                .imageUrl(b.getBarber().getImageUrl())
                                                .build() : null)
                // Service
                .service(b.getService() != null ? ServiceInfo.builder()
                                                  .id(b.getService().getId())
                                                  .name(b.getService().getName())
                                                  .price(b.getService().getPrice())
                                                  .durationMinutes(b.getService().getDurationMinutes())
                                                  .build() : null)
                .bookingTime(b.getBookingTime() != null ? b.getBookingTime().toString() : null)
                .bookingEndTime(b.getBookingEndTime() != null ? b.getBookingEndTime().toString() : null)
                .status(b.getStatus().name())
                .note(b.getNote() != null ? b.getNote() : "")
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null)
                .updatedAt(b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : null)
                .cancelledAt(b.getCancelledAt() != null ? b.getCancelledAt().toString() : null)
                .cancelReason(b.getCancelReason())
                .review(review != null ? ReviewInfo.builder()
                                         .rating(review.getRating())
                                         .comment(review.getComment() != null ? review.getComment() : "")
                                         .build() : null)
                .build();
    }
}