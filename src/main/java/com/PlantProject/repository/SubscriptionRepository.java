package com.PlantProject.repository;

import com.PlantProject.model.Subscription;
import com.PlantProject.PlantProject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Subscription findByRazorpayOrderId(String orderId);
    Subscription findByUserAndStatusAndEndDateAfter(User user, String status, LocalDateTime date);
} 