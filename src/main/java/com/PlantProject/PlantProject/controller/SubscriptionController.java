package com.PlantProject.PlantProject.controller;

import com.PlantProject.PlantProject.model.Subscription;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.service.RazorpayService;
import com.PlantProject.PlantProject.service.SubscriptionService;
import com.PlantProject.PlantProject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<?> createOrder(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String planType = request.get("plan");
            User user = userService.getCurrentUser();

            logger.info("Creating order for user: {} with plan: {}", user.getEmail(), planType);

            Map<String, Object> orderDetails = razorpayService.createOrder(planType);
            
            // Create a pending subscription
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlanType(planType);
            subscription.setAmount(Double.parseDouble(orderDetails.get("amount").toString()) / 100);
            subscription.setStatus("pending");
            subscription.setRazorpayOrderId(orderDetails.get("orderId").toString());
            subscription.setStartDate(LocalDateTime.now());
            
            // Set end date based on plan type
            switch (planType.toLowerCase()) {
                case "monthly":
                    subscription.setEndDate(LocalDateTime.now().plusMonths(1));
                    break;
                case "quarterly":
                    subscription.setEndDate(LocalDateTime.now().plusMonths(3));
                    break;
                case "halfyearly":
                    subscription.setEndDate(LocalDateTime.now().plusMonths(6));
                    break;
            }
            
            subscriptionService.saveSubscription(subscription);
            logger.info("Created pending subscription for order: {}", orderDetails.get("orderId"));

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderDetails.get("orderId"));
            response.put("amount", orderDetails.get("amount"));
            response.put("currency", orderDetails.get("currency"));
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating order", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/verify-payment")
    @ResponseBody
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String orderId = request.get("razorpay_order_id");
            String paymentId = request.get("razorpay_payment_id");
            String signature = request.get("razorpay_signature");

            logger.info("Verifying payment for order: {}", orderId);

            boolean isValid = razorpayService.verifyPayment(orderId, paymentId, signature);

            if (isValid) {
                // Update subscription status
                Subscription subscription = subscriptionService.findByOrderId(orderId);
                if (subscription != null) {
                    subscription.setStatus("active");
                    subscription.setRazorpayPaymentId(paymentId);
                    subscription.setRazorpaySignature(signature);
                    subscriptionService.saveSubscription(subscription);
                    logger.info("Payment verified and subscription activated for order: {}", orderId);
                } else {
                    logger.error("Subscription not found for order: {}", orderId);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Payment verified successfully");
                return ResponseEntity.ok(response);
            } else {
                logger.error("Payment verification failed for order: {}", orderId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Payment verification failed");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error verifying payment", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error verifying payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/subscription/status")
    @ResponseBody
    public ResponseEntity<?> getSubscriptionStatus(Authentication authentication) {
        try {
            User user = userService.getCurrentUser();
            Subscription subscription = subscriptionService.getActiveSubscription(user);
            
            Map<String, Object> response = new HashMap<>();
            if (subscription != null) {
                response.put("hasActiveSubscription", true);
                response.put("planType", subscription.getPlanType());
                response.put("endDate", subscription.getEndDate());
            } else {
                response.put("hasActiveSubscription", false);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting subscription status", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 