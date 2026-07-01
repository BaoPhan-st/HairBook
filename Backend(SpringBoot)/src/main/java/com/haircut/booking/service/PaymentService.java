package com.haircut.booking.service;

import com.haircut.booking.entity.Booking;
import com.haircut.booking.entity.Payment;
import com.haircut.booking.repository.BookingRepository;
import com.haircut.booking.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${vnpay.return-url:hairbook://payment}")
    private String vnpayReturnUrl;

    @Value("${momo.return-url:hairbook://payment}")
    private String momoReturnUrl;

    public Map<String, Object> createPayment(Long bookingId, String method, String returnUrl) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        Payment existing = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (existing != null && existing.getStatus() == Payment.Status.SUCCESS)
            throw new IllegalArgumentException("Booking này đã được thanh toán");

        String orderId = "HB" + System.currentTimeMillis();
        Double amount = booking.getService().getPrice();
        String methodEnum = method.toUpperCase();

        String paymentUrl = buildPaymentUrl(methodEnum, orderId, amount,
                returnUrl != null ? returnUrl : vnpayReturnUrl);

        // Nếu booking đã có payment PENDING/FAILED từ trước -> cập nhật lại bản ghi đó
        // thay vì tạo mới (booking_id là UNIQUE, insert mới sẽ vi phạm ràng buộc)
        Payment payment = existing != null ? existing : Payment.builder().booking(booking).build();
        payment.setAmount(amount);
        payment.setMethod(Payment.Method.valueOf(methodEnum));
        payment.setStatus(Payment.Status.PENDING);
        payment.setOrderId(orderId);
        payment.setPaymentUrl(paymentUrl);
        payment.setPaidAt(null);

        paymentRepository.save(payment);

        return Map.of(
                "paymentUrl", paymentUrl,
                "orderId", orderId,
                "amount", amount,
                "method", methodEnum,
                "status", "PENDING"
        );
    }

    public Map<String, Object> handleCallback(Map<String, String> params) {
        String orderId = params.getOrDefault("vnp_TxnRef",
                params.getOrDefault("orderId", ""));
        String resultCode = params.getOrDefault("vnp_ResponseCode",
                params.getOrDefault("resultCode", "99"));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order không tồn tại: " + orderId));

        boolean success = "00".equals(resultCode) || "0".equals(resultCode);
        payment.setStatus(success ? Payment.Status.SUCCESS : Payment.Status.FAILED);
        if (success) payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return Map.of(
                "orderId", orderId,
                "status", payment.getStatus().name(),
                "amount", payment.getAmount()
        );
    }

    public Map<String, Object> getPaymentStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order không tồn tại"));

        return Map.of(
                "orderId", orderId,
                "status", payment.getStatus().name(),
                "method", payment.getMethod().name(),
                "amount", payment.getAmount()
        );
    }

    private String buildPaymentUrl(String method, String orderId,
                                   Double amount, String returnUrl) {
        long amountLong = amount.longValue();
        if ("VNPAY".equals(method)) {
            return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                    + "?vnp_Version=2.1.0"
                    + "&vnp_Command=pay"
                    + "&vnp_TmnCode=HAIRBOOK"
                    + "&vnp_Amount=" + (amountLong * 100)
                    + "&vnp_CurrCode=VND"
                    + "&vnp_TxnRef=" + orderId
                    + "&vnp_OrderInfo=Thanh+toan+lich+cat+toc"
                    + "&vnp_OrderType=other"
                    + "&vnp_Locale=vn"
                    + "&vnp_ReturnUrl=" + returnUrl
                    + "&vnp_IpAddr=127.0.0.1"
                    + "&vnp_CreateDate=" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "&vnp_SecureHash=MOCK_HASH_" + UUID.randomUUID();
        } else {
            return "momo://app"
                    + "?action=payWithApp"
                    + "&isScanQR=false"
                    + "&partnerCode=HAIRBOOK"
                    + "&orderId=" + orderId
                    + "&amount=" + amountLong
                    + "&orderInfo=Thanh+toan+lich+cat+toc"
                    + "&returnUrl=" + returnUrl
                    + "&notifyUrl=" + returnUrl
                    + "&requestId=" + UUID.randomUUID()
                    + "&requestType=captureMoMoWallet";
        }
    }
}