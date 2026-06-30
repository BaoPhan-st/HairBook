package com.haircut.booking.repository;

import com.haircut.booking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Admin queries
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    long countByRole(User.Role role);
    long countByStatus(User.Status status);
}