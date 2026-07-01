package com.haircut.booking.controller;

import com.haircut.booking.service.ReviewService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Data
    public static class ReviewRequest {
        private Long bookingId;
        private int rating;
        private String comment;
    }

    @PostMapping
    public ResponseEntity<?> submitReview(
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> result = reviewService.submitReview(
                    req.getBookingId(),
                    req.getRating(),
                    req.getComment(),
                    userDetails.getUsername()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getReviewByBooking(@PathVariable Long bookingId) {
        Map<String, Object> review = reviewService.getReviewByBooking(bookingId);
        if (review == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(review);
    }
}
