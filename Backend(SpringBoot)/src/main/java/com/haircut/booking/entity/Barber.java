package com.haircut.booking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "barbers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "specialty", length = 255)
    private String specialty;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;
}