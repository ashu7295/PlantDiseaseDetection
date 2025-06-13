package com.PlantProject.PlantProject.service;

import com.PlantProject.PlantProject.model.AnalysisResult;
import com.PlantProject.PlantProject.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AnalysisService {
    /**
     * Analyzes a plant image and returns the analysis result
     * @param file The image file to analyze
     * @param user The user performing the analysis
     * @return AnalysisResult containing the analysis details
     */
    AnalysisResult analyzePlant(MultipartFile file, User user);

    /**
     * Analyzes an image and returns the analysis result
     * @param file The image file to analyze
     * @param user The user performing the analysis
     * @return AnalysisResult containing the analysis details
     */
    AnalysisResult analyzeImage(MultipartFile file, User user);

    /**
     * Saves an analysis result to the database
     * @param result The analysis result to save
     * @return The saved analysis result
     */
    AnalysisResult saveAnalysisResult(AnalysisResult result);

    /**
     * Gets recent analyses for a user
     * @param user The user to get analyses for
     * @return List of recent analysis results
     */
    List<AnalysisResult> getRecentAnalyses(User user);

    /**
     * Gets analysis statistics for a user
     * @param user The user to get statistics for
     * @return Map containing analysis statistics
     */
    Map<String, Object> getUserAnalysisStats(User user);
} 