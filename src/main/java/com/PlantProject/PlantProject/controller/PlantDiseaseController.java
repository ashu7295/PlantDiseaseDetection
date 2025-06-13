package com.PlantProject.PlantProject.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.model.AnalysisResult;
import com.PlantProject.PlantProject.service.AnalysisService;
import com.PlantProject.PlantProject.service.UserService;
import com.PlantProject.PlantProject.service.SubscriptionService;

@RestController
@RequestMapping("/api")
public class PlantDiseaseController {
    private static final Logger logger = LoggerFactory.getLogger(PlantDiseaseController.class);

    @Value("${python.app.path}")
    private String pythonAppPath;
    
    @Value("${upload.path}")
    private String uploadPath;
    
    @Value("${python.executable.path}")
    private String pythonPath;
    
    @Autowired
    private AnalysisService analysisService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping(value = "/analyze", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> analyzePlant(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "plantType", required = true) String plantType) {
        logger.info("/api/analyze called with plantType={} and image={} (size={})", plantType, image != null ? image.getOriginalFilename() : null, image != null ? image.getSize() : 0);
        try {
            // Get current user from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            logger.info("User for analysis: {}", user != null ? user.getEmail() : "null");
            // Analyze the plant (pass plantType)
            AnalysisResult analysisResult = analysisService.analyzePlant(image, plantType, user);
            logger.info("Analysis result: {} {} {} {}", analysisResult.getPlantName(), analysisResult.getDisease(), analysisResult.getConfidence(), analysisResult.getImagePath());
            // Create response object
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("imagePath", analysisResult.getImagePath());
            response.put("disease", analysisResult.getDisease());
            response.put("confidence", analysisResult.getConfidence());
            response.put("plantName", analysisResult.getPlantName());
            response.put("analysisDate", analysisResult.getAnalysisDate());
            logger.info("Returning analysis response for user {}", user != null ? user.getEmail() : "null");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (Exception e) {
            logger.error("Failed to analyze plant image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "status", "error",
                    "error", e.getMessage()
                ));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getAnalysisHistory(@RequestParam Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        return ResponseEntity.ok(analysisService.getRecentAnalyses(user));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAnalysisStats(@RequestParam Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        Map<String, Object> stats = analysisService.getUserAnalysisStats(user);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveAnalysis(
            @RequestParam Long userId,
            @RequestParam String plantName,
            @RequestParam String disease,
            @RequestParam Double confidence) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            AnalysisResult analysisResult = analysisService.createAnalysis(user, plantName, disease, confidence);
            return ResponseEntity.ok(analysisResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save analysis: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // Detailed authentication logging
            logger.info("========== User Profile Request ==========");
            logger.info("Authentication details:");
            logger.info("Is auth null? {}", auth == null);
            logger.info("Auth class: {}", auth != null ? auth.getClass().getName() : "null");
            logger.info("Auth name: {}", auth != null ? auth.getName() : "null");
            logger.info("Principal class: {}", auth != null && auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
            logger.info("Principal details: {}", auth != null ? auth.getPrincipal() : "null");
            logger.info("Is authenticated? {}", auth != null ? auth.isAuthenticated() : "false");
            logger.info("Authorities: {}", auth != null ? auth.getAuthorities() : "null");
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("User not authenticated - auth: {}, name: {}", auth, auth != null ? auth.getName() : "null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "status", "error",
                        "message", "User not authenticated"
                    ));
            }
            
            String email = auth.getName();
            logger.info("Looking for user with email: {}", email);
            User user = userService.findByEmail(email);
            
            if (user == null) {
                logger.error("User not found in database with email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "status", "error",
                        "message", "User not found"
                    ));
            }

            logger.info("Found user: {} (id: {})", user.getEmail(), user.getId());
            logger.info("User details - name: {}, email: {}", user.getName(), user.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("name", user.getName());
            profile.put("email", user.getEmail());
            profile.put("joinDate", user.getCreatedAt());
            profile.put("phoneNumber", user.getPhoneNumber());
            profile.put("address", user.getAddress());
            profile.put("city", user.getCity());
            profile.put("state", user.getState());
            profile.put("postalCode", user.getPostalCode());
            profile.put("country", user.getCountry());
            profile.put("profilePicture", user.getProfilePicture());
            
            boolean hasSubscription = subscriptionService.hasActiveSubscription(user);
            logger.info("User subscription status: {}", hasSubscription);
            profile.put("hasActiveSubscription", hasSubscription);
            
            // Get user's analysis stats
            Map<String, Object> stats = analysisService.getUserAnalysisStats(user);
            profile.put("stats", stats);
            
            response.put("user", profile);
            
            logger.info("Returning profile response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error fetching user profile: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.ok(new ArrayList<>()); // Return empty array for unauthenticated users
            }
            
            User user = userService.findByEmail(auth.getName());
            if (user == null) {
                return ResponseEntity.ok(new ArrayList<>()); // Return empty array if user not found
            }

            // For now, return empty notifications array
            // In a real application, you would fetch notifications from a notifications service
            List<Map<String, Object>> notifications = new ArrayList<>();
            
            // You can add sample notifications here for testing
            // notifications.add(Map.of(
            //     "id", 1,
            //     "title", "Analysis Complete",
            //     "message", "Your plant analysis has been completed",
            //     "type", "success",
            //     "read", false,
            //     "createdAt", LocalDateTime.now().toString()
            // ));
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error fetching notifications", e);
            return ResponseEntity.ok(new ArrayList<>()); // Return empty array on error
        }
    }

    @GetMapping("/analyze")
    public String analyzeGetInfo() {
        return "This endpoint only supports POST requests for image analysis. Please use POST with an image file.";
    }

    @GetMapping("/debug/auth")
    public ResponseEntity<?> debugAuth() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Map<String, Object> debugInfo = new HashMap<>();
            
            debugInfo.put("authExists", auth != null);
            debugInfo.put("authName", auth != null ? auth.getName() : "null");
            debugInfo.put("authPrincipal", auth != null ? auth.getPrincipal().toString() : "null");
            debugInfo.put("authClass", auth != null ? auth.getClass().getSimpleName() : "null");
            debugInfo.put("isAuthenticated", auth != null ? auth.isAuthenticated() : false);
            debugInfo.put("authorities", auth != null ? auth.getAuthorities().toString() : "null");
            
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = userService.findByEmail(auth.getName());
                debugInfo.put("userFound", user != null);
                debugInfo.put("userName", user != null ? user.getName() : "null");
                debugInfo.put("userEmail", user != null ? user.getEmail() : "null");
            }
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Error in debug auth", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
} 