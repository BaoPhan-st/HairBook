package com.haircut.booking.service;

import com.haircut.booking.entity.User;
import com.haircut.booking.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    @Value("${fcm.server-key:}")
    private String fcmServerKey;

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    public void saveFcmToken(String email, String fcmToken) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // user.setFcmToken(fcmToken);
            // userRepository.save(user);
            log.info("FCM token saved for {}: {}", email, fcmToken);
        });
    }

    public void sendToDevice(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmServerKey == null || fcmServerKey.isEmpty()) {
            log.warn("FCM server key chưa được cấu hình — bỏ qua gửi notification");
            return;
        }
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM token rỗng — bỏ qua");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + fcmServerKey);

        Map<String, Object> notification = Map.of(
                "title", title,
                "body", body
        );

        Map<String, Object> payload = data != null
                ? Map.of("to", fcmToken, "notification", notification, "data", data)
                : Map.of("to", fcmToken, "notification", notification);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(FCM_URL, entity, String.class);
            log.info("FCM response: {}", response.getBody());
        } catch (Exception e) {
            log.error("Gửi FCM thất bại: {}", e.getMessage());
        }
    }

    public void sendBookingReminder(String fcmToken, String barberName,
                                    String serviceName, String time) {
        sendToDevice(
                fcmToken,
                "✂️ Nhắc lịch cắt tóc – " + serviceName,
                "Lịch hẹn với " + barberName + " lúc " + time + " sắp đến!",
                Map.of("type", "REMINDER")
        );
    }
}
