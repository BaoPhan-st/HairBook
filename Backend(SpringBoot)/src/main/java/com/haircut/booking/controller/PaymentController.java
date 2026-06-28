package com.haircut.booking.controller;

import com.haircut.booking.service.PaymentService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @Data
    public static class PaymentRequest {
        private Long bookingId;
        private String method;
        private String returnUrl;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestBody PaymentRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> result = paymentService.createPayment(
                    req.getBookingId(),
                    req.getMethod(),
                    req.getReturnUrl()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> paymentCallback(@RequestParam Map<String, String> params) {
        try {
            Map<String, Object> result = paymentService.handleCallback(params);
            String status = result.get("status").toString();
            String orderId = result.get("orderId").toString();

            String deeplink = "hairbook://payment?status="
                    + ("SUCCESS".equals(status) ? "success" : "failed")
                    + "&orderId=" + orderId;

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", deeplink)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "hairbook://payment?status=failed")
                    .build();
        }
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String orderId) {
        try {
            return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
