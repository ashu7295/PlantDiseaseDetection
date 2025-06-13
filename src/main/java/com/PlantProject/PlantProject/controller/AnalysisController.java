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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

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
    public String showAnalyzePage(Model model) {
        // Same as /analyze for consistency
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userService.findByEmail(email);
        if (user != null) {
            model.addAttribute("userId", user.getId());
            List<AnalysisResult> recentAnalyses = analysisService.getRecentAnalyses(user);
            if (recentAnalyses.size() > 6) {
                recentAnalyses = recentAnalyses.subList(0, 6);
            }
            model.addAttribute("recentAnalyses", recentAnalyses);
        }
        return "analyze";
    }

    @GetMapping("/analyze")
    public String showAnalyzePageAlias(Model model) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userService.findByEmail(email);
        if (user != null) {
            model.addAttribute("userId", user.getId());
            // Get 6 most recent analyses
            List<AnalysisResult> recentAnalyses = analysisService.getRecentAnalyses(user);
            if (recentAnalyses.size() > 6) {
                recentAnalyses = recentAnalyses.subList(0, 6);
            }
            model.addAttribute("recentAnalyses", recentAnalyses);
        }
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

    @GetMapping("/history")
    public String showAnalysisHistoryPage(Model model) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userService.findByEmail(email);
        if (user != null) {
            List<AnalysisResult> analyses = analysisService.getRecentAnalyses(user);
            model.addAttribute("analyses", analyses);
        } else {
            model.addAttribute("analyses", Collections.emptyList());
        }
        return "analysis-history";
    }

    @GetMapping(value = "/api/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getAnalysisHistory() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "User not found"));
            }
            List<AnalysisResult> recentAnalyses = analysisService.getRecentAnalyses(user);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(recentAnalyses);
        } catch (Exception e) {
            logger.error("Error fetching analysis history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Error fetching analysis history: " + e.getMessage()));
        }
    }

    @GetMapping(value = "/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getAnalysisStats() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "User not found"));
            }
            Map<String, Object> stats = analysisService.getUserAnalysisStats(user);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(stats);
        } catch (Exception e) {
            logger.error("Error fetching analysis stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Error fetching analysis stats: " + e.getMessage()));
        }
    }

    @GetMapping("/api/limits")
    @ResponseBody
    public ResponseEntity<?> getAnalysisLimits() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            
            Map<String, Object> limits = new HashMap<>();
            limits.put("hasActiveSubscription", analysisService.hasActiveSubscription(user));
            limits.put("freeAnalysesUsed", user.getFreeAnalysesUsed() != null ? user.getFreeAnalysesUsed() : 0);
            limits.put("freeAnalysesLimit", user.getFreeAnalysesLimit() != null ? user.getFreeAnalysesLimit() : 10);
            limits.put("remainingFreeAnalyses", analysisService.getRemainingFreeAnalyses(user));
            limits.put("canPerformAnalysis", analysisService.canUserPerformAnalysis(user));
            
            return ResponseEntity.ok(limits);
        } catch (Exception e) {
            logger.error("Error fetching analysis limits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching analysis limits: " + e.getMessage()));
        }
    }
} 