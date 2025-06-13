package com.PlantProject.PlantProject.repository;

import com.PlantProject.PlantProject.model.OTP;
import com.PlantProject.PlantProject.model.OTPType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByEmailAndOtpAndTypeAndUsedFalse(String email, String otp, OTPType type);
    void deleteByEmailAndType(String email, OTPType type);
} 