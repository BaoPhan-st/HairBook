package com.haircut.booking.controller;

import com.haircut.booking.entity.HaircutService;
import com.haircut.booking.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @GetMapping
    public List<HaircutService> getAllServices() {
        return serviceRepository.findAll();
    }
}