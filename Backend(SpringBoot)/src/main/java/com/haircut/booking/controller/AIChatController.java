package com.haircut.booking.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIChatController {

    @Value("${gemini.api.key:MOCK}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate;

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String reply;
        private boolean fromAI;

        public ChatResponse(String reply, boolean fromAI) {
            this.reply = reply;
            this.fromAI = fromAI;
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("AI Chat request from user: {} | message: {}", userDetails.getUsername(), request.getMessage());

        if ("MOCK".equals(geminiApiKey) || geminiApiKey.startsWith("YOUR_")) {
            return ResponseEntity.ok(mockResponse(request.getMessage()));
        }

        try {
            String aiReply = callGeminiAPI(request.getMessage());
            return ResponseEntity.ok(new ChatResponse(aiReply, true));
        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage());
            return ResponseEntity.ok(mockResponse(request.getMessage()));
        }
    }

    private String callGeminiAPI(String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        String systemContext = """
        Bạn là trợ lý AI của tiệm cắt tóc. Nhiệm vụ của bạn:
        1. Tư vấn kiểu tóc phù hợp với khuôn mặt, phong cách
        2. Giải thích các dịch vụ: cắt tóc, gội đầu, nhuộm, uốn
        3. Gợi ý thời gian đặt lịch phù hợp
        4. Trả lời ngắn gọn, thân thiện bằng tiếng Việt
        Câu hỏi của khách: """;

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", systemContext + userMessage)
                ))
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String targetUrl = geminiApiUrl + "/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        log.info("Calling Gemini URL: {}", targetUrl);

        Map<String, Object> response = restTemplate.postForObject(targetUrl, entity, Map.class);
        return parseGeminiResponse(response);
    }


    private String parseGeminiResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại!";
        }
    }

    private ChatResponse mockResponse(String message) {
        String lower = message.toLowerCase();
        String reply;

        if (lower.contains("khuôn mặt tròn") || lower.contains("mặt tròn")) {
            reply = "Với khuôn mặt tròn, bạn nên chọn kiểu tóc tạo chiều cao như:\n" +
                    "• Undercut với phần đỉnh dài\n• Pompadour\n• Faux Hawk\n\n" +
                    "Tránh các kiểu tóc ngang tai vì sẽ làm mặt tròn hơn. Bạn muốn đặt lịch tư vấn trực tiếp không? 😊";
        } else if (lower.contains("giá") || lower.contains("bao nhiêu")) {
            reply = "Bảng giá dịch vụ của chúng tôi:\n" +
                    "• Cắt tóc nam: 50.000đ\n• Cắt + Gội: 80.000đ\n" +
                    "• Nhuộm tóc: từ 200.000đ\n• Uốn tóc: từ 300.000đ\n\n" +
                    "Bạn muốn đặt lịch dịch vụ nào?";
        } else if (lower.contains("thợ") || lower.contains("barber")) {
            reply = "Tiệm hiện có 3 thợ:\n" +
                    "• Anh Minh — chuyên tóc nam Hàn Quốc ⭐ 4.9\n" +
                    "• Anh Tuấn — chuyên undercut, fade ⭐ 4.8\n" +
                    "• Chị Lan — chuyên tóc nữ, nhuộm ⭐ 4.7\n\n" +
                    "Bạn muốn đặt lịch với ai?";
        } else if (lower.contains("đặt lịch") || lower.contains("book")) {
            reply = "Để đặt lịch, bạn có thể:\n" +
                    "1. Nhấn nút 'Đặt lịch ngay' trên màn hình chính\n" +
                    "2. Chọn dịch vụ → Chọn thợ → Chọn ngày giờ\n\n" +
                    "Thời gian mở cửa: 8:00 - 20:00 mỗi ngày 📅";
        } else {
            reply = "Xin chào! Tôi là trợ lý AI của tiệm cắt tóc 💇\n\n" +
                    "Tôi có thể giúp bạn:\n" +
                    "• Tư vấn kiểu tóc phù hợp\n" +
                    "• Thông tin giá dịch vụ\n" +
                    "• Giới thiệu các thợ\n" +
                    "• Hỗ trợ đặt lịch\n\n" +
                    "Bạn cần tư vấn gì ạ?";
        }

        return new ChatResponse(reply, false);
    }
}
