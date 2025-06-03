package com.PlantProject.controller;

import com.PlantProject.model.Subscription;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.service.RazorpayService;
import com.PlantProject.service.SubscriptionService;
import com.PlantProject.PlantProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class SubscriptionController {

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

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderDetails.get("orderId"));
            response.put("amount", orderDetails.get("amount"));
            response.put("currency", orderDetails.get("currency"));
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
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

            boolean isValid = razorpayService.verifyPayment(orderId, paymentId, signature);

            if (isValid) {
                // Update subscription status
                Subscription subscription = subscriptionService.findByOrderId(orderId);
                if (subscription != null) {
                    subscription.setStatus("active");
                    subscription.setRazorpayPaymentId(paymentId);
                    subscription.setRazorpaySignature(signature);
                    subscriptionService.saveSubscription(subscription);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Payment verification failed");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
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
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 