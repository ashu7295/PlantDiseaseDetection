package com.PlantProject.PlantProject.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {
    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing Razorpay client with key ID: {}", keyId);
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
            logger.info("Razorpay client initialized successfully");
        } catch (RazorpayException e) {
            logger.error("Failed to initialize Razorpay client", e);
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    public Map<String, Object> createOrder(String planType) throws RazorpayException {
        logger.info("Creating order for plan type: {}", planType);
        JSONObject options = new JSONObject();
        int amount = getAmountForPlan(planType);
        options.put("amount", amount);
        options.put("currency", "INR");
        options.put("receipt", "receipt_" + System.currentTimeMillis());

        logger.debug("Order creation options: {}", options.toString());
        Order order = razorpayClient.orders.create(options);
        logger.info("Order created successfully with ID: {}", order.get("id").toString());

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("amount", order.get("amount"));
        response.put("currency", order.get("currency"));
        response.put("receipt", order.get("receipt"));

        return response;
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        logger.info("Verifying payment for order ID: {}", orderId);
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            
            logger.debug("Payment verification attributes: {}", attributes.toString());
            boolean isValid = com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret);
            logger.info("Payment verification result for order {}: {}", orderId, isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Payment verification failed for order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    private int getAmountForPlan(String planType) {
        int amount;
        switch (planType.toLowerCase()) {
            case "monthly":
                amount = 29900; // ₹299
                break;
            case "quarterly":
                amount = 79900; // ₹799
                break;
            case "halfyearly":
                amount = 149900; // ₹1499
                break;
            default:
                logger.error("Invalid plan type: {}", planType);
                throw new IllegalArgumentException("Invalid plan type");
        }
        logger.debug("Amount for plan {}: {}", planType, amount);
        return amount;
    }
} 