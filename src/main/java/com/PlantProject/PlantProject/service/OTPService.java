package com.PlantProject.PlantProject.service;

import com.PlantProject.PlantProject.model.OTP;
import com.PlantProject.PlantProject.model.OTPType;
import com.PlantProject.PlantProject.repository.OTPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OTPService {
    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

    @Autowired
    private OTPRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public void generateAndSendOTP(String email, OTPType type) throws Exception {
        logger.info("Generating OTP for email: {} and type: {}", email, type);
        
        try {
            // Delete any existing OTPs for this email and type
            otpRepository.deleteByEmailAndType(email, type);
            logger.debug("Deleted existing OTPs for email: {} and type: {}", email, type);

            // Generate new OTP
            String otp = String.format("%0" + otpLength + "d", random.nextInt((int) Math.pow(10, otpLength)));
            logger.debug("Generated OTP for email: {}", email);
            
            // Create and save OTP entity
            OTP otpEntity = new OTP();
            otpEntity.setEmail(email);
            otpEntity.setOtp(otp);
            otpEntity.setType(type);
            otpEntity.setCreatedAt(LocalDateTime.now());
            otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
            otpEntity.setUsed(false);
            otpRepository.save(otpEntity);
            logger.debug("Saved OTP entity to database");

            // Send OTP via email
            String subject = type == OTPType.EMAIL_VERIFICATION ? 
                "Email Verification OTP" : "Password Reset OTP";
            emailService.sendOTPEmail(email, otp, subject);
            logger.info("Successfully sent OTP email to: {}", email);
        } catch (Exception e) {
            logger.error("Error generating and sending OTP for email: {} and type: {}", email, type, e);
            throw e;
        }
    }

    @Transactional
    public boolean validateOTP(String email, String otp, OTPType type) {
        logger.info("Validating OTP for email: {} and type: {}", email, type);
        
        try {
            Optional<OTP> otpEntity = otpRepository.findByEmailAndOtpAndTypeAndUsedFalse(email, otp, type);
            
            if (otpEntity.isPresent()) {
                OTP otpObj = otpEntity.get();
                
                // Check if OTP has expired
                if (LocalDateTime.now().isAfter(otpObj.getExpiresAt())) {
                    logger.warn("OTP has expired for email: {}", email);
                    return false;
                }
                
                // Mark OTP as used
                otpObj.setUsed(true);
                otpRepository.save(otpObj);
                logger.info("OTP validated successfully for email: {}", email);
                return true;
            }
            
            logger.warn("Invalid OTP provided for email: {}", email);
            return false;
        } catch (Exception e) {
            logger.error("Error validating OTP for email: {} and type: {}", email, type, e);
            return false;
        }
    }

    @Transactional
    public void deleteExpiredOTPs() {
        logger.info("Starting cleanup of expired OTPs");
        try {
            LocalDateTime now = LocalDateTime.now();
            otpRepository.findAll().stream()
                .filter(otp -> now.isAfter(otp.getExpiresAt()))
                .forEach(otp -> {
                    otpRepository.delete(otp);
                    logger.debug("Deleted expired OTP for email: {}", otp.getEmail());
                });
            logger.info("Completed cleanup of expired OTPs");
        } catch (Exception e) {
            logger.error("Error during cleanup of expired OTPs", e);
        }
    }
} 