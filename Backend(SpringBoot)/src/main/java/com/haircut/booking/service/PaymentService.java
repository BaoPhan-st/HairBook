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

    /**
     * Tạo payment record + trả về URL deeplink để app mở VNPay/MoMo.
     *
     * Thực tế cần tích hợp SDK VNPay / MoMo để tạo chữ ký (HMAC).
     * Ở đây xây dựng URL giả lập đúng format — thay bằng SDK thật khi deploy.
     */
    public Map<String, Object> createPayment(Long bookingId, String method, String returnUrl) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        // Nếu đã có payment thành công thì không tạo thêm
        paymentRepository.findByBookingId(bookingId).ifPresent(existing -> {
            if (existing.getStatus() == Payment.Status.SUCCESS)
                throw new IllegalArgumentException("Booking này đã được thanh toán");
        });

        String orderId    = "HB" + System.currentTimeMillis();
        Double amount     = booking.getService().getPrice();
        String methodEnum = method.toUpperCase();

        String paymentUrl = buildPaymentUrl(methodEnum, orderId, amount,
                returnUrl != null ? returnUrl : vnpayReturnUrl);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .method(Payment.Method.valueOf(methodEnum))
                .status(Payment.Status.PENDING)
                .orderId(orderId)
                .paymentUrl(paymentUrl)
                .build();

        paymentRepository.save(payment);

        return Map.of(
                "paymentUrl", paymentUrl,
                "orderId",    orderId,
                "amount",     amount,
                "method",     methodEnum,
                "status",     "PENDING"
        );
    }

    /**
     * Callback từ VNPay/MoMo sau khi user thanh toán.
     * VNPay gọi GET /api/payments/callback?vnp_ResponseCode=00&vnp_TxnRef=HB123...
     * MoMo  gọi GET /api/payments/callback?resultCode=0&orderId=HB123...
     */
    public Map<String, Object> handleCallback(Map<String, String> params) {
        String orderId    = params.getOrDefault("vnp_TxnRef",
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
                "status",  payment.getStatus().name(),
                "amount",  payment.getAmount()
        );
    }

    /** Lấy trạng thái thanh toán theo orderId */
    public Map<String, Object> getPaymentStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order không tồn tại"));

        return Map.of(
                "orderId", orderId,
                "status",  payment.getStatus().name(),
                "method",  payment.getMethod().name(),
                "amount",  payment.getAmount()
        );
    }

    // ── private ──────────────────────────────────────────────────────────────

    private String buildPaymentUrl(String method, String orderId,
                                   Double amount, String returnUrl) {
        long amountLong = amount.longValue();
        if ("VNPAY".equals(method)) {
            // Format URL theo VNPay sandbox — thay bằng chữ ký HMAC thật khi production
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
            // MoMo deeplink format
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
