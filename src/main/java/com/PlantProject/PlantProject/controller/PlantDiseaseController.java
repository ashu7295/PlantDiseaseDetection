package com.PlantProject.PlantProject.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.io.IOException;
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
import com.PlantProject.PlantProject.model.Analysis;
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
            @RequestParam("userId") Long userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            Analysis analysis = analysisService.analyzePlant(image, user);
            return ResponseEntity.ok(analysis);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to process image: " + e.getMessage());
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

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalAnalyses", analysisService.countTotalAnalyses(user));
        stats.put("healthyPlants", analysisService.countHealthyPlants(user));
        stats.put("diseasedPlants", analysisService.countDiseasedPlants(user));

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

            Analysis analysis = analysisService.saveAnalysis(user, plantName, disease, confidence);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save analysis: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "status", "error",
                        "message", "User not found"
                    ));
            }

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
            Map<String, Long> stats = analysisService.getUserAnalysisStats(user);
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

    @PutMapping("/user/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> updates) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "status", "error",
                        "message", "User not found"
                    ));
            }
            // Update fields if present in request
            if (updates.containsKey("name")) user.setName((String) updates.get("name"));
            if (updates.containsKey("phoneNumber")) user.setPhoneNumber((String) updates.get("phoneNumber"));
            if (updates.containsKey("address")) user.setAddress((String) updates.get("address"));
            if (updates.containsKey("city")) user.setCity((String) updates.get("city"));
            if (updates.containsKey("state")) user.setState((String) updates.get("state"));
            if (updates.containsKey("postalCode")) user.setPostalCode((String) updates.get("postalCode"));
            if (updates.containsKey("country")) user.setCountry((String) updates.get("country"));
            if (updates.containsKey("profilePicture")) user.setProfilePicture((String) updates.get("profilePicture"));
            // Do NOT update password here unless explicitly provided and handled securely
            userService.saveUser(user);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error updating user profile: " + e.getMessage()
                ));
        }
    }
} 