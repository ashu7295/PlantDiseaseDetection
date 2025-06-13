package com.PlantProject.PlantProject.service.impl;

import com.PlantProject.PlantProject.model.AnalysisResult;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.repository.AnalysisResultRepository;
import com.PlantProject.PlantProject.service.AnalysisService;
import com.PlantProject.PlantProject.service.EmailService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalysisServiceImpl implements AnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private EmailService emailService;

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public AnalysisResult analyzePlant(MultipartFile file, User user) {
        try {
            // Save the uploaded file
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);
            Files.createDirectories(filePath.getParent());
            Files.copy(file.getInputStream(), filePath);

            // TODO: Implement actual plant disease detection logic here
            // For now, return a mock result
            AnalysisResult result = new AnalysisResult();
            result.setUserId(user.getId());
            result.setImagePath(fileName);
            result.setPlantName("Sample Plant");
            result.setDisease("Sample Disease");
            result.setConfidence(0.95);
            result.setAnalysisDate(LocalDateTime.now());

            // Save the result
            result = saveAnalysisResult(result);

            // Send email notification
            emailService.sendAnalysisNotification(user.getEmail(), result, fileName);

            return result;
        } catch (IOException | MessagingException e) {
            logger.error("Error analyzing plant image", e);
            throw new RuntimeException("Failed to analyze plant image", e);
        }
    }

    @Override
    public AnalysisResult analyzeImage(MultipartFile file, User user) {
        // For now, just call analyzePlant as they do the same thing
        return analyzePlant(file, user);
    }

    @Override
    public AnalysisResult saveAnalysisResult(AnalysisResult result) {
        return analysisResultRepository.save(result);
    }

    @Override
    public List<AnalysisResult> getRecentAnalyses(User user) {
        return analysisResultRepository.findByUserIdOrderByAnalysisDateDesc(user.getId());
    }

    @Override
    public Map<String, Object> getUserAnalysisStats(User user) {
        List<AnalysisResult> analyses = getRecentAnalyses(user);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalAnalyses", analyses.size());
        stats.put("recentAnalyses", analyses.subList(0, Math.min(5, analyses.size())));
        
        // Calculate average confidence
        double avgConfidence = analyses.stream()
                .mapToDouble(AnalysisResult::getConfidence)
                .average()
                .orElse(0.0);
        stats.put("averageConfidence", avgConfidence);
        
        return stats;
    }
} 