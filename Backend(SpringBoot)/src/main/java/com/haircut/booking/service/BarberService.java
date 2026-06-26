package com.haircut.booking.service;

import com.haircut.booking.dto.BarberRequest;
import com.haircut.booking.entity.Barber;
import com.haircut.booking.repository.BarberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BarberService {

    private final BarberRepository barberRepository;

    public List<Barber> getAll() {
        return barberRepository.findAll();
    }

    public Barber getById(Long id) {
        return barberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thợ id=" + id));
    }

    public Barber create(BarberRequest req) {
        Barber barber = Barber.builder()
                .name(req.getName())
                .specialty(req.getSpecialty())
                .imageUrl(req.getImageUrl())
                .rating(req.getRating() != null ? req.getRating() : 0.0)
                .available(req.getAvailable() != null ? req.getAvailable() : true)
                .build();
        return barberRepository.save(barber);
    }

    public Barber update(Long id, BarberRequest req) {
        Barber barber = getById(id);
        if (req.getName()      != null) barber.setName(req.getName());
        if (req.getSpecialty() != null) barber.setSpecialty(req.getSpecialty());
        if (req.getImageUrl()  != null) barber.setImageUrl(req.getImageUrl());
        if (req.getRating()    != null) barber.setRating(req.getRating());
        if (req.getAvailable() != null) barber.setAvailable(req.getAvailable());
        return barberRepository.save(barber);
    }

    public void delete(Long id) {
        if (!barberRepository.existsById(id))
            throw new IllegalArgumentException("Không tìm thấy thợ id=" + id);
        barberRepository.deleteById(id);
    }
}