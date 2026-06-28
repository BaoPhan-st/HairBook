package com.haircut.booking.controller;

import com.haircut.booking.entity.HaircutService;
import com.haircut.booking.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        List<ServiceResponse> response = serviceRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                HaircutService::getId,
                                Comparator.nullsLast(Long::compareTo)
                        )
                )
                .map(this::toServiceResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private ServiceResponse toServiceResponse(HaircutService service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.getImageUrl()
        );
    }

    public record ServiceResponse(
            Long id,
            String name,
            String description,
            Double price,
            Integer durationMinutes,
            String imageUrl
    ) {
    }
}