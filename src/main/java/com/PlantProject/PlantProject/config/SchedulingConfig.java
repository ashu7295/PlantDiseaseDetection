package com.PlantProject.PlantProject.config;

import com.PlantProject.PlantProject.service.OTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);
    
    @Autowired
    private OTPService otpService;
    
    /**
     * Clean up expired OTPs every 30 minutes
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutes in milliseconds
    public void cleanupExpiredOTPs() {
        logger.info("Starting scheduled cleanup of expired OTPs");
        try {
            otpService.cleanupExpiredOTPs();
            logger.info("Completed scheduled cleanup of expired OTPs");
        } catch (Exception e) {
            logger.error("Error during scheduled cleanup of expired OTPs", e);
        }
    }
    
    /**
     * Log system health every hour
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1 hour in milliseconds
    public void logSystemHealth() {
        logger.info("System health check - Application is running normally");
    }
} 