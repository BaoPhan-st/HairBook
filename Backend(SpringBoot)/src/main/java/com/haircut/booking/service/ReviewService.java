package com.haircut.booking.service;

import com.haircut.booking.entity.Barber;
import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.Review;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BarberRepository;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import com.haircut.booking.service.UserService;
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final BarberRepository barberRepository;
    private final UserService userService;

    public Map<String, Object> submitReview(Long bookingId, int rating, String comment, String email) {
        User user = userService.findByEmail(email);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        if (!booking.getUser().getId().equals(user.getId()))
            throw new IllegalArgumentException("Bạn không có quyền đánh giá booking này");

        if (booking.getStatus() != Booking.Status.COMPLETED)
            throw new IllegalArgumentException("Chỉ có thể đánh giá booking đã hoàn thành");

        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");

        Review review = reviewRepository.findByBookingId(bookingId)
                .orElseGet(() -> Review.builder().booking(booking).user(user).build());

        review.setRating(rating);
        review.setComment(comment);

        Map<String, Object> dto = toDto(reviewRepository.save(review));

        // Cập nhật lại điểm rating trung bình của barber ngay sau khi lưu review
        updateBarberRating(booking.getBarber());

        return dto;
    }

    private void updateBarberRating(Barber barber) {
        if (barber == null) return;
        Double avg = reviewRepository.findAverageRatingByBarberId(barber.getId());
        double rounded = avg == null ? 0.0
                : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        barber.setRating(rounded);
        barberRepository.save(barber);
    }

    public Map<String, Object> getReviewByBooking(Long bookingId) {
        Optional<Review> review = reviewRepository.findByBookingId(bookingId);
        if (review.isEmpty()) return null;
        return toDto(review.get());
    }

    private Map<String, Object> toDto(Review r) {
        return Map.of(
                "id", r.getId(),
                "bookingId", r.getBooking().getId(),
                "rating", r.getRating(),
                "comment", r.getComment() != null ? r.getComment() : "",
                "createdAt", r.getCreatedAt().toString()
        );
    }
}