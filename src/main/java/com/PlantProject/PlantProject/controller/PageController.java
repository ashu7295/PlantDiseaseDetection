package com.PlantProject.PlantProject.controller;

import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.service.AnalysisService;
import com.PlantProject.PlantProject.service.UserService;
import com.PlantProject.PlantProject.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @PostMapping("/api/contact")
    @ResponseBody
    public ResponseEntity<?> submitContactForm(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message) {
        
        try {
            // Validate input
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Subject is required"));
            }
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
            }

            // Send contact form email
            try {
                emailService.sendContactFormNotification(name, email, subject, message);
                logger.info("Contact form submitted successfully by {} ({})", name, email);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Thank you for your message! We will get back to you soon.");
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                logger.error("Failed to send contact form email", e);
                return ResponseEntity.status(500).body(Map.of("error", "Failed to send message. Please try again later."));
            }
            
        } catch (Exception e) {
            logger.error("Error processing contact form", e);
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred. Please try again later."));
        }
    }
} 