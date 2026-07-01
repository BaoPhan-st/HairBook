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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Có lỗi xảy ra khi tạo thanh toán, vui lòng thử lại"));
        }
    }

    @GetMapping(value = "/mock-checkout", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mockCheckout(
            @RequestParam String orderId,
            @RequestParam long amount,
            @RequestParam String method) {

        String logoText = "VNPAY".equals(method) ? "VNPAY (Demo)" : "MoMo (Demo)";
        String amountText = String.format("%,d", amount).replace(",", ".") + " đ";

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>" + logoText + "</title>"
                + "<style>"
                + "body{font-family:sans-serif;background:#f5f5f5;display:flex;justify-content:center;"
                + "align-items:center;height:100vh;margin:0}"
                + ".box{background:#fff;padding:24px;border-radius:12px;box-shadow:0 2px 10px rgba(0,0,0,.1);"
                + "width:90%;max-width:360px;text-align:center}"
                + "h2{margin-top:0;color:#333}"
                + ".amount{font-size:24px;font-weight:bold;color:#1976D2;margin:16px 0}"
                + ".note{font-size:12px;color:#999;margin-bottom:20px}"
                + "button{width:100%;padding:14px;border:none;border-radius:8px;font-size:16px;"
                + "margin-bottom:10px;cursor:pointer}"
                + ".ok{background:#4CAF50;color:#fff}"
                + ".fail{background:#F44336;color:#fff}"
                + "</style></head><body>"
                + "<div class='box'>"
                + "<h2>" + logoText + "</h2>"
                + "<div class='note'>Đơn hàng: " + orderId + "</div>"
                + "<div class='amount'>" + amountText + "</div>"
                + "<div class='note'>Đây là trang giả lập cổng thanh toán dùng cho mục đích demo đồ án"
                + " (chưa kết nối cổng thanh toán thật)</div>"
                + "<button class='ok' onclick=\"location.href='/api/payments/callback?orderId="
                + orderId + "&vnp_ResponseCode=00'\">✅ Giả lập thanh toán thành công</button>"
                + "<button class='fail' onclick=\"location.href='/api/payments/callback?orderId="
                + orderId + "&vnp_ResponseCode=97'\">❌ Giả lập thanh toán thất bại</button>"
                + "</div></body></html>";

        return ResponseEntity.ok(html);
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