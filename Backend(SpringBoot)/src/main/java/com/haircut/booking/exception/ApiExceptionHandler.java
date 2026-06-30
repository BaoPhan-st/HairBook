package com.haircut.booking.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ApiExceptionHandler.class
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        Map<String, Object> body = createErrorBody(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không hợp lệ."
        );

        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        Map<String, Object> body = createErrorBody(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không hợp lệ."
        );

        body.put("details", exception.getMessage());

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException exception
    ) {
        Map<String, Object> body = createErrorBody(
                HttpStatus.BAD_REQUEST,
                "JSON request không hợp lệ hoặc thiếu dữ liệu cần thiết."
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();

        String message = exception.getReason();

        if (message == null || message.isBlank()) {
            message = "Yêu cầu không thể được xử lý.";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", statusCode.value());
        body.put("error", message);

        return ResponseEntity.status(statusCode).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        LOGGER.warn(
                "Database integrity violation: {}",
                exception.getMostSpecificCause().getMessage()
        );

        Map<String, Object> body = createErrorBody(
                HttpStatus.CONFLICT,
                "Dữ liệu không thể được lưu vì có xung đột trong database."
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception exception
    ) {
        LOGGER.error("Unexpected server error", exception);

        Map<String, Object> body = createErrorBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Máy chủ gặp lỗi không mong muốn. Vui lòng thử lại sau."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    private Map<String, Object> createErrorBody(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);

        return body;
    }
}