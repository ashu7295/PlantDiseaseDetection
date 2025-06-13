package com.PlantProject.PlantProject.controller;

import com.PlantProject.PlantProject.dto.SignupRequest;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully. Please check your email for OTP verification.");
            response.put("email", registeredUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "An error occurred during registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            
            if (email == null || otp == null) {
                return ResponseEntity.badRequest().body("Email and OTP are required");
            }
            
            boolean verified = userService.verifyEmail(email, otp);
            if (verified) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Email verified successfully");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body("Invalid OTP");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            userService.initiatePasswordReset(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset OTP sent to your email");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String newPassword = request.get("newPassword");
            
            if (email == null || otp == null || newPassword == null) {
                return ResponseEntity.badRequest().body("Email, OTP, and new password are required");
            }
            
            boolean reset = userService.resetPassword(email, otp, newPassword);
            if (reset) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Password reset successfully");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body("Invalid OTP");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
        try {
            logger.info("Received profile update request with data: {}", updates);
            
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.info("Authentication status: isAuthenticated={}, principal={}", 
                auth != null && auth.isAuthenticated(), 
                auth != null ? auth.getName() : "null");
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("Unauthorized profile update attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "User not authenticated"));
            }
            
            // Get current user
            String email = auth.getName();
            logger.info("Looking up user with email: {}", email);
            User currentUser = userService.findByEmail(email);
            
            if (currentUser == null) {
                logger.error("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "User not found"));
            }
            
            logger.info("Found user: id={}, name={}", currentUser.getId(), currentUser.getName());
            
            // Update user fields
            boolean hasUpdates = false;
            if (updates.containsKey("name")) {
                logger.info("Updating name from '{}' to '{}'", currentUser.getName(), updates.get("name"));
                currentUser.setName(updates.get("name"));
                hasUpdates = true;
            }
            if (updates.containsKey("phoneNumber")) {
                logger.info("Updating phone from '{}' to '{}'", currentUser.getPhoneNumber(), updates.get("phoneNumber"));
                currentUser.setPhoneNumber(updates.get("phoneNumber"));
                hasUpdates = true;
            }
            if (updates.containsKey("address")) {
                logger.info("Updating address from '{}' to '{}'", currentUser.getAddress(), updates.get("address"));
                currentUser.setAddress(updates.get("address"));
                hasUpdates = true;
            }
            if (updates.containsKey("city")) {
                logger.info("Updating city from '{}' to '{}'", currentUser.getCity(), updates.get("city"));
                currentUser.setCity(updates.get("city"));
                hasUpdates = true;
            }
            if (updates.containsKey("state")) {
                logger.info("Updating state from '{}' to '{}'", currentUser.getState(), updates.get("state"));
                currentUser.setState(updates.get("state"));
                hasUpdates = true;
            }
            if (updates.containsKey("postalCode")) {
                logger.info("Updating postal code from '{}' to '{}'", currentUser.getPostalCode(), updates.get("postalCode"));
                currentUser.setPostalCode(updates.get("postalCode"));
                hasUpdates = true;
            }
            if (updates.containsKey("country")) {
                logger.info("Updating country from '{}' to '{}'", currentUser.getCountry(), updates.get("country"));
                currentUser.setCountry(updates.get("country"));
                hasUpdates = true;
            }
            
            if (!hasUpdates) {
                logger.warn("No fields to update in request");
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "No fields to update"));
            }
            
            // Save user
            logger.info("Attempting to save updated user profile");
            User savedUser = userService.saveUser(currentUser);
            logger.info("Successfully saved user profile. Updated user: id={}, name={}", savedUser.getId(), savedUser.getName());
            
            // Return success response
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Profile updated successfully",
                "user", savedUser
            ));
        } catch (Exception e) {
            logger.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
} 