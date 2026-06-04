package com.haircut.booking.controller;

import com.haircut.booking.entity.User;
import com.haircut.booking.security.JwtUtil;
import com.haircut.booking.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String email;
        private String password;
        private String fullName;
        private String phone;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String email;
        private String fullName;
        public AuthResponse(String token, String email, String fullName) {
            this.token = token; this.email = email; this.fullName = fullName;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email hoặc mật khẩu không đúng"));
        }
        User user = userService.findByEmail(req.getEmail());
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getFullName()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            User user = userService.register(
                    req.getEmail(), req.getPassword(), req.getFullName(), req.getPhone()
            );
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(token, user.getEmail(), user.getFullName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(Map.of(
                "id",       user.getId(),
                "email",    user.getEmail(),
                "fullName", user.getFullName(),
                "phone",    user.getPhone() != null ? user.getPhone() : ""
        ));
    }
}