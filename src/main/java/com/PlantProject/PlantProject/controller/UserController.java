package com.PlantProject.PlantProject.controller;

import com.PlantProject.PlantProject.dto.SignupRequest;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully. Please check your email for OTP verification.");
            response.put("email", registeredUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "An error occurred during registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

    @PostMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(user);
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("name", updatedUser.getName());
            profile.put("email", updatedUser.getEmail());
            profile.put("phoneNumber", updatedUser.getPhoneNumber());
            profile.put("address", updatedUser.getAddress());
            profile.put("city", updatedUser.getCity());
            profile.put("state", updatedUser.getState());
            profile.put("postalCode", updatedUser.getPostalCode());
            profile.put("country", updatedUser.getCountry());
            profile.put("userRole", updatedUser.getUserRole());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("profile", profile);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 