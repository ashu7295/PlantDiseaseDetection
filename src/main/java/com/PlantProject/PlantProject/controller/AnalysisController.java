package com.PlantProject.PlantProject.controller;

import com.PlantProject.PlantProject.model.Analysis;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.model.AnalysisResult;
import com.PlantProject.PlantProject.service.AnalysisService;
import com.PlantProject.PlantProject.service.EmailService;
import com.PlantProject.PlantProject.service.UserService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UserService userService;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

    @Autowired
    public AnalysisController(AnalysisService analysisService, UserService userService, EmailService emailService) {
        this.analysisService = analysisService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/page")
    public String showAnalyzePage() {
        return "analyze";
    }

    @PostMapping("/analyze")
    @ResponseBody
    public ResponseEntity<?> analyzePlant(@RequestParam("image") MultipartFile image,
                                        @RequestParam(value = "userId", required = false) Long userId) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select an image"));
            }

            User user = null;
            if (userId != null) {
                user = userService.findById(userId);
                if (user == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
                }
            }

            AnalysisResult result = analysisService.analyzeImage(image, user);
            
            if (user != null) {
                // Save analysis result
                analysisService.saveAnalysisResult(result);
                
                // Send email notification
                try {
                    emailService.sendAnalysisNotification(user.getEmail(), result, image.getOriginalFilename());
                } catch (Exception e) {
                    logger.error("Failed to send analysis notification email", e);
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error analyzing plant image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error analyzing image: " + e.getMessage()));
        }
    }

    @GetMapping("/recent")
    @ResponseBody
    public ResponseEntity<?> getRecentAnalyses(@RequestParam("userId") Long userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            List<AnalysisResult> recentAnalyses = analysisService.getRecentAnalyses(user);
            return ResponseEntity.ok(recentAnalyses);
        } catch (Exception e) {
            logger.error("Error fetching recent analyses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching recent analyses: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<?> getAnalysisStats(@RequestParam("userId") Long userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            Map<String, Object> stats = analysisService.getUserAnalysisStats(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching analysis stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching analysis stats: " + e.getMessage()));
        }
    }
} 