package com.PlantProject.PlantProject.repository;

import com.PlantProject.PlantProject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderId(String providerId);
    boolean existsByEmail(String email);
} 