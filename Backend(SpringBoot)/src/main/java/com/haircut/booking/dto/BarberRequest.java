package com.haircut.booking.dto;

import lombok.Data;

@Data
public class BarberRequest {
    private String name;
    private String specialty;
    private String imageUrl;
    private Double rating;
    private Boolean available;
}