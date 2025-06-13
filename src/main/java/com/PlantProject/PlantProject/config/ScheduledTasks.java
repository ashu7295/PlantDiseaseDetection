package com.PlantProject.PlantProject.config;

import com.PlantProject.PlantProject.service.OTPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTasks {

    @Autowired
    private OTPService otpService;

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredOTPs() {
        otpService.cleanupExpiredOTPs();
    }
} 