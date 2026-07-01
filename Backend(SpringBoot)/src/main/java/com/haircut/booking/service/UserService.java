package com.haircut.booking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haircut.booking.entity.User;
import com.haircut.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.clientId}")
    private String googleClientId;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public User register(String email, String rawPassword, String fullName, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .phone(phone)
                .build();

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * Xác minh Google ID Token qua Google tokeninfo endpoint (không cần thư viện ngoài).
     * Tìm user theo email hoặc tạo mới nếu chưa có.
     */
    public User loginOrRegisterWithGoogle(String idTokenString) {
        try {
            // Gọi Google tokeninfo API để xác minh token
            URL url = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                throw new IllegalArgumentException("Google ID Token không hợp lệ (HTTP " + statusCode + ")");
            }

            // Đọc response JSON
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JsonNode payload = objectMapper.readTree(sb.toString());

            // Kiểm tra audience (aud) để đảm bảo token thuộc app này
            String aud = payload.get("aud").asText();
            if (!aud.equals(googleClientId)) {
                throw new IllegalArgumentException("Token không thuộc ứng dụng này");
            }

            String email    = payload.get("email").asText();
            String fullName = payload.has("name") ? payload.get("name").asText() : email.split("@")[0];

            // Tìm user đã có hoặc tạo mới
            String finalFullName = fullName;
            return userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .email(email)
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .fullName(finalFullName)
                                .phone("")
                                .build();
                        return userRepository.save(newUser);
                    });

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác minh Google Token: " + e.getMessage(), e);
        }
    }
}
