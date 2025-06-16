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

    @PostMapping("/analyze")
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
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to analyze plant image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
            logger.info("Authentication object: {}", auth);
            logger.info("Authentication name: {}", auth != null ? auth.getName() : "null");
            logger.info("Authentication principal: {}", auth != null ? auth.getPrincipal() : "null");
            logger.info("Authentication authorities: {}", auth != null ? auth.getAuthorities() : "null");
            logger.info("Is authenticated: {}", auth != null ? auth.isAuthenticated() : "false");
            
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
            // Get user's analysis stats
            Map<String, Object> stats = analysisService.getUserAnalysisStats(user);
            profile.put("stats", stats);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error fetching user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error fetching user profile: " + e.getMessage()
                ));
        }
    }

    @PostMapping(value = "/user/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PutMapping(value = "/user/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> updates) {
        try {
            logger.info("Received profile update request with data: {}", updates);
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.info("Authentication: {}, isAuthenticated: {}", auth, auth != null ? auth.isAuthenticated() : "null");
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "status", "error",
                        "message", "User not authenticated"
                    ));
            }
            
            User user = userService.findByEmail(auth.getName());
            logger.info("Found user: {}", user != null ? user.getEmail() : "null");
            
            if (user == null) {
                logger.warn("User not found in database");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "status", "error",
                        "message", "User not found"
                    ));
            }
            
            // Update user fields
            logger.info("Updating user fields...");
            if (updates.containsKey("name")) {
                logger.info("Updating name from '{}' to '{}'", user.getName(), updates.get("name"));
                user.setName((String) updates.get("name"));
            }
            if (updates.containsKey("phoneNumber")) {
                logger.info("Updating phoneNumber from '{}' to '{}'", user.getPhoneNumber(), updates.get("phoneNumber"));
                user.setPhoneNumber((String) updates.get("phoneNumber"));
            }
            if (updates.containsKey("address")) {
                logger.info("Updating address from '{}' to '{}'", user.getAddress(), updates.get("address"));
                user.setAddress((String) updates.get("address"));
            }
            if (updates.containsKey("city")) {
                logger.info("Updating city from '{}' to '{}'", user.getCity(), updates.get("city"));
                user.setCity((String) updates.get("city"));
            }
            if (updates.containsKey("state")) {
                logger.info("Updating state from '{}' to '{}'", user.getState(), updates.get("state"));
                user.setState((String) updates.get("state"));
            }
            if (updates.containsKey("postalCode")) {
                logger.info("Updating postalCode from '{}' to '{}'", user.getPostalCode(), updates.get("postalCode"));
                user.setPostalCode((String) updates.get("postalCode"));
            }
            if (updates.containsKey("country")) {
                logger.info("Updating country from '{}' to '{}'", user.getCountry(), updates.get("country"));
                user.setCountry((String) updates.get("country"));
            }
            if (updates.containsKey("profilePicture")) {
                logger.info("Updating profilePicture");
                user.setProfilePicture((String) updates.get("profilePicture"));
            }

            // Save user
            logger.info("Saving updated user...");
            User savedUser = userService.saveUser(user);
            
            // Get user's analysis stats
            Map<String, Object> stats = analysisService.getUserAnalysisStats(savedUser);
            
            // Create response with updated profile data
            Map<String, Object> profile = new HashMap<>();
            profile.put("name", savedUser.getName());
            profile.put("email", savedUser.getEmail());
            profile.put("joinDate", savedUser.getCreatedAt());
            profile.put("phoneNumber", savedUser.getPhoneNumber());
            profile.put("address", savedUser.getAddress());
            profile.put("city", savedUser.getCity());
            profile.put("state", savedUser.getState());
            profile.put("postalCode", savedUser.getPostalCode());
            profile.put("country", savedUser.getCountry());
            profile.put("profilePicture", savedUser.getProfilePicture());
            profile.put("userRole", savedUser.getUserRole());
            profile.put("stats", stats);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("profile", profile);
            
            logger.info("Profile updated successfully for user: {}", savedUser.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error updating profile: " + e.getMessage()
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