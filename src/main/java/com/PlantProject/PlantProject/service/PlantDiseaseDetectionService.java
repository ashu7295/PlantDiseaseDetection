package com.PlantProject.PlantProject.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class PlantDiseaseDetectionService {
    
    private static final String PYTHON_SCRIPT_PATH = "src/main/python/plant_disease_detector.py";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public JsonNode detectDisease(MultipartFile imageFile, String plantType) {
        try {
            // Save the uploaded file temporarily
            Path tempFile = Files.createTempFile("plant_image_", ".jpg");
            Files.copy(imageFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Build the command to run the Python script
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3",
                PYTHON_SCRIPT_PATH,
                tempFile.toString(),
                plantType.toLowerCase()
            );
            
            // Set working directory to where the model file is located
            processBuilder.directory(new File("src/main/python"));
            
            // Start the process
            Process process = processBuilder.start();
            
            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            
            // Wait for the process to complete
            int exitCode = process.waitFor();
            
            // Clean up the temporary file
            Files.delete(tempFile);
            
            if (exitCode == 0 && result != null) {
                return objectMapper.readTree(result);
            } else {
                throw new RuntimeException("Python script execution failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error during disease detection: " + e.getMessage(), e);
        }
    }
} 