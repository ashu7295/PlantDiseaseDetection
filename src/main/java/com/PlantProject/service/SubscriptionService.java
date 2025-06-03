package com.PlantProject.service;

import com.PlantProject.model.Subscription;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public Subscription saveSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public Subscription findByOrderId(String orderId) {
        return subscriptionRepository.findByRazorpayOrderId(orderId);
    }

    public Subscription getActiveSubscription(User user) {
        return subscriptionRepository.findByUserAndStatusAndEndDateAfter(
            user, 
            "active", 
            LocalDateTime.now()
        );
    }

    public boolean hasActiveSubscription(User user) {
        return getActiveSubscription(user) != null;
    }
} 