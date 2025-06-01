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
    
    private static final String PYTHON_PATH = "/Library/Frameworks/Python.framework/Versions/3.12/bin/python3";
    
    @Autowired
    private AnalysisService analysisService;
    
    @Autowired
    private UserService userService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("plantType") String plantType) {
        
        logger.info("Received request to analyze image for plant type: {}", plantType);
        
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Please select an image"
            ));
        }

        File tempFile = null;
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            
            // Save the uploaded file
            String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            Path uploadDir = Path.of(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Save temporary file for Python processing
            tempFile = File.createTempFile("plant_", "_" + image.getOriginalFilename());
            image.transferTo(tempFile);
            logger.info("Saved temporary image file: {}", tempFile.getAbsolutePath());

            // Build the command to run the Python script
            String[] command = {
                PYTHON_PATH,
                pythonAppPath + "/plant_disease_detector.py",
                tempFile.getAbsolutePath(),
                plantType
            };
            logger.info("Executing command: {}", String.join(" ", command));

            // Execute the Python script
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(pythonAppPath)); // Use the python app directory
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read the output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Python output: {}", line);
                    if (line.trim().startsWith("{")) {
                        output.append(line.trim());
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            logger.info("Python script exited with code: {}", exitCode);

            if (exitCode != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "error",
                        "message", "Error analyzing image",
                        "output", output.toString()
                    ));
            }

            // Parse the JSON output
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(output.toString(), Map.class);
            
            // Save analysis result
            List<List<Double>> confidenceScores = (List<List<Double>>) result.get("confidence_scores");
            Double confidence = confidenceScores.get(0).stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
                
            analysisService.saveAnalysis(
                user,
                plantType,
                (String) result.get("prediction"),
                confidence,
                fileName
            );
            
            // Add imagePath to the result so frontend can display the image
            result.put("imagePath", fileName);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error analyzing image: " + e.getMessage()
                ));
        } finally {
            // Clean up the temporary file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                logger.info("Cleaned up temporary file");
            }
        }
    }
    
    @GetMapping("/analysis/history")
    public ResponseEntity<?> getAnalysisHistory() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            
            List<Analysis> history = analysisService.getUserAnalysisHistory(user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching analysis history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error fetching analysis history: " + e.getMessage()
                ));
        }
    }
    
    @GetMapping("/analysis/stats")
    public ResponseEntity<?> getAnalysisStats() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            
            Map<String, Long> stats = analysisService.getUserAnalysisStats(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching analysis stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Error fetching analysis stats: " + e.getMessage()
                ));
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
} 