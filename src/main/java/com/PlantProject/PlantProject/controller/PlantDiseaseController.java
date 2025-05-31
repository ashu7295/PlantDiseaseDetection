package com.PlantProject.PlantProject.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class PlantDiseaseController {
    private static final Logger logger = LoggerFactory.getLogger(PlantDiseaseController.class);

    @Value("${python.app.path}")
    private String pythonAppPath;

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
            // Save the uploaded file temporarily
            tempFile = File.createTempFile("plant_", "_" + image.getOriginalFilename());
            image.transferTo(tempFile);
            logger.info("Saved temporary image file: {}", tempFile.getAbsolutePath());

            // Build the command to run the Python script
            String[] command = {
                "python3",
                pythonAppPath + "/plant_disease_detector.py",
                tempFile.getAbsolutePath(),
                plantType
            };
            logger.info("Executing command: {}", String.join(" ", command));

            // Execute the Python script
            ProcessBuilder processBuilder = new ProcessBuilder(command);
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
} 