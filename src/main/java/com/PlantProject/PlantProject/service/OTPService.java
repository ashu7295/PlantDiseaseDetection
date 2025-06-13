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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OTPService {
    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

    @Autowired
    private OTPRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Value("${otp.expiration.minutes:10}")
    private int otpExpirationMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.max.attempts:3}")
    private int maxOtpAttempts;

    @Value("${otp.rate.limit.per.hour:5}")
    private int maxOtpRequestsPerHour;

    private static final SecureRandom random = new SecureRandom();
    
    // Rate limiting maps
    private final ConcurrentHashMap<String, AtomicInteger> otpRequestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastRequestTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    @Transactional
    public void generateAndSendOTP(String email, OTPType type) throws Exception {
        logger.info("Generating OTP for email: {} and type: {}", email, type);
        
        try {
            // Check rate limiting
            if (!checkRateLimit(email)) {
                throw new RuntimeException("Too many OTP requests. Please try again later.");
            }
            
            // Delete any existing OTPs for this email and type
            otpRepository.deleteByEmailAndType(email, type);
            logger.debug("Deleted existing OTPs for email: {} and type: {}", email, type);

            // Generate new OTP
            String otp = generateSecureOTP();
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
            emailService.sendOTPEmail(email, otp);
            
            // Update rate limiting counters
            updateRateLimit(email);
            
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
            // Check if too many failed attempts
            if (hasExceededMaxAttempts(email)) {
                logger.warn("Too many failed OTP attempts for email: {}", email);
                return false;
            }
            
            Optional<OTP> otpEntityOpt = otpRepository.findByEmailAndOtpAndTypeAndUsedFalse(email, otp, type);
            
            if (otpEntityOpt.isEmpty()) {
                recordFailedAttempt(email);
                logger.warn("Invalid OTP provided for email: {} and type: {}", email, type);
                return false;
            }
            
            OTP otpEntity = otpEntityOpt.get();
            
            // Check if OTP has expired
            if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
                recordFailedAttempt(email);
                logger.warn("Expired OTP used for email: {} and type: {}", email, type);
                return false;
            }
            
            // Mark OTP as used
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            
            // Clear failed attempts on successful validation
            clearFailedAttempts(email);
            
            logger.info("Successfully validated OTP for email: {} and type: {}", email, type);
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating OTP for email: {} and type: {}", email, type, e);
            recordFailedAttempt(email);
            return false;
        }
    }

    /**
     * Generates a cryptographically secure OTP
     */
    private String generateSecureOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Checks if the email has exceeded the rate limit for OTP requests
     */
    private boolean checkRateLimit(String email) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // Clean up old entries
        lastRequestTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(oneHourAgo));
        otpRequestCounts.entrySet().removeIf(entry -> 
            !lastRequestTimes.containsKey(entry.getKey()));
        
        // Check current count
        AtomicInteger count = otpRequestCounts.get(email);
        if (count != null && count.get() >= maxOtpRequestsPerHour) {
            LocalDateTime lastRequest = lastRequestTimes.get(email);
            if (lastRequest != null && lastRequest.isAfter(oneHourAgo)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Updates the rate limiting counters
     */
    private void updateRateLimit(String email) {
        LocalDateTime now = LocalDateTime.now();
        lastRequestTimes.put(email, now);
        otpRequestCounts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Checks if the email has exceeded maximum failed attempts
     */
    private boolean hasExceededMaxAttempts(String email) {
        AtomicInteger attempts = failedAttempts.get(email);
        return attempts != null && attempts.get() >= maxOtpAttempts;
    }

    /**
     * Records a failed OTP attempt
     */
    private void recordFailedAttempt(String email) {
        failedAttempts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Clears failed attempts for successful validation
     */
    private void clearFailedAttempts(String email) {
        failedAttempts.remove(email);
    }

    /**
     * Cleanup expired OTPs (should be called periodically)
     */
    @Transactional
    public void cleanupExpiredOTPs() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<OTP> expiredOTPs = otpRepository.findAll().stream()
                .filter(otp -> otp.getExpiresAt().isBefore(now))
                .toList();
            
            if (!expiredOTPs.isEmpty()) {
                otpRepository.deleteAll(expiredOTPs);
                logger.info("Cleaned up {} expired OTPs", expiredOTPs.size());
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired OTPs", e);
        }
    }
} 