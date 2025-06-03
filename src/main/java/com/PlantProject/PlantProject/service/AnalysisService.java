package com.PlantProject.PlantProject.service;

import com.PlantProject.PlantProject.model.Analysis;
import com.PlantProject.PlantProject.model.User;
import com.PlantProject.PlantProject.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AnalysisService {
    
    @Autowired
    private AnalysisRepository analysisRepository;
    
    public Analysis saveAnalysis(User user, String plantType, String prediction, Double confidence, String imagePath) {
        Analysis analysis = new Analysis();
        analysis.setUser(user);
        analysis.setPlantType(plantType);
        analysis.setPrediction(prediction);
        analysis.setConfidence(confidence);
        analysis.setImagePath(imagePath);
        analysis.setAnalysisDate(LocalDateTime.now());
        return analysisRepository.save(analysis);
    }
    
    public List<Analysis> getUserAnalysisHistory(User user) {
        return analysisRepository.findTop6ByUserOrderByAnalysisDateDesc(user);
    }
    
    public Map<String, Long> getUserAnalysisStats(User user) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalAnalyses", analysisRepository.countTotalAnalyses(user));
        stats.put("healthyPlants", analysisRepository.countHealthyPlants(user));
        stats.put("diseasedPlants", analysisRepository.countDiseasedPlants(user));
        return stats;
    }
} 