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
     * @param plantType The type of plant
     * @param user The user performing the analysis
     * @return AnalysisResult containing the analysis details
     */
    AnalysisResult analyzePlant(MultipartFile file, String plantType, User user);

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
     * Creates and saves a new analysis result
     * @param user The user performing the analysis
     * @param plantName The name of the plant
     * @param disease The disease detected
     * @param confidence The confidence level
     * @return The created and saved analysis result
     */
    AnalysisResult createAnalysis(User user, String plantName, String disease, Double confidence);

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

    /**
     * Checks if user can perform analysis (has remaining free analyses or active subscription)
     * @param user The user to check
     * @return true if user can perform analysis, false otherwise
     */
    boolean canUserPerformAnalysis(User user);

    /**
     * Gets remaining free analyses for a user
     * @param user The user to check
     * @return Number of remaining free analyses
     */
    int getRemainingFreeAnalyses(User user);

    /**
     * Increments the user's free analysis count
     * @param user The user whose count to increment
     */
    void incrementFreeAnalysisCount(User user);

    /**
     * Checks if user has active subscription
     * @param user The user to check
     * @return true if user has active subscription, false otherwise
     */
    boolean hasActiveSubscription(User user);
} 