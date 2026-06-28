package com.haircut.booking.service;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.Review;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;

    public Map<String, Object> submitReview(Long bookingId, int rating, String comment, String email) {
        User user = userService.findByEmail(email);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        if (!booking.getUser().getId().equals(user.getId()))
            throw new IllegalArgumentException("Bạn không có quyền đánh giá booking này");

        if (booking.getStatus() != Booking.Status.COMPLETED)
            throw new IllegalArgumentException("Chỉ có thể đánh giá booking đã hoàn thành");

        if (reviewRepository.existsByBookingId(bookingId))
            throw new IllegalArgumentException("Booking này đã được đánh giá");

        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");

        Review review = Review.builder()
                .booking(booking)
                .user(user)
                .rating(rating)
                .comment(comment)
                .build();

        return toDto(reviewRepository.save(review));
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
