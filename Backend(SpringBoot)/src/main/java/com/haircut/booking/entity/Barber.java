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

    @Column(nullable = false)
    private String name;

    private String specialty;

    private String imageUrl;

    private Double rating;

    @Builder.Default
    private Boolean available = true;
}