package com.PlantProject.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    public RazorpayService() {
        try {
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> createOrder(String planType) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("amount", getAmountForPlan(planType));
        options.put("currency", "INR");
        options.put("receipt", "receipt_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(options);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("amount", order.get("amount"));
        response.put("currency", order.get("currency"));
        response.put("receipt", order.get("receipt"));

        return response;
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            // Create the attributes object
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            
            // Verify the signature
            return com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getAmountForPlan(String planType) {
        switch (planType.toLowerCase()) {
            case "monthly":
                return 29900; // ₹299
            case "quarterly":
                return 79900; // ₹799
            case "halfyearly":
                return 149900; // ₹1499
            default:
                throw new IllegalArgumentException("Invalid plan type");
        }
    }
} 