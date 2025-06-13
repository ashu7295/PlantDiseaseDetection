package com.PlantProject.PlantProject.service;

import com.PlantProject.PlantProject.model.Subscription;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public Subscription saveSubscription(Subscription subscription) {
        try {
            logger.info("Saving subscription for user: {}, plan: {}, status: {}", 
                subscription.getUser().getEmail(), 
                subscription.getPlanType(), 
                subscription.getStatus());
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            logger.info("Successfully saved subscription with ID: {}", savedSubscription.getId());
            return savedSubscription;
        } catch (Exception e) {
            logger.error("Error saving subscription: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Subscription findByOrderId(String orderId) {
        try {
            logger.info("Finding subscription by order ID: {}", orderId);
            Subscription subscription = subscriptionRepository.findByRazorpayOrderId(orderId);
            if (subscription == null) {
                logger.warn("No subscription found for order ID: {}", orderId);
            } else {
                logger.info("Found subscription with ID: {} for order ID: {}", subscription.getId(), orderId);
            }
            return subscription;
        } catch (Exception e) {
            logger.error("Error finding subscription by order ID: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Subscription getActiveSubscription(User user) {
        try {
            logger.info("Getting active subscription for user: {}", user.getEmail());
            Subscription subscription = subscriptionRepository.findByUserAndStatusAndEndDateAfter(
                user, 
                "active", 
                LocalDateTime.now()
            );
            if (subscription == null) {
                logger.info("No active subscription found for user: {}", user.getEmail());
            } else {
                logger.info("Found active subscription with ID: {} for user: {}", subscription.getId(), user.getEmail());
            }
            return subscription;
        } catch (Exception e) {
            logger.error("Error getting active subscription: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean hasActiveSubscription(User user) {
        try {
            logger.info("Checking if user has active subscription: {}", user.getEmail());
            boolean hasActive = getActiveSubscription(user) != null;
            logger.info("User {} has active subscription: {}", user.getEmail(), hasActive);
            return hasActive;
        } catch (Exception e) {
            logger.error("Error checking active subscription: {}", e.getMessage(), e);
            throw e;
        }
    }
} 