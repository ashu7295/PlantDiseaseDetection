package com.PlantProject.PlantProject.repository;

import com.PlantProject.PlantProject.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByUserIdOrderByAnalysisDateDesc(Long userId);
    
    // Statistical methods
    long countByUserId(Long userId);
    
    @Query("SELECT COUNT(ar) FROM AnalysisResult ar WHERE ar.userId = ?1 AND LOWER(ar.disease) LIKE '%healthy%'")
    long countHealthyPlantsByUserId(Long userId);
    
    @Query("SELECT COUNT(ar) FROM AnalysisResult ar WHERE ar.userId = ?1 AND LOWER(ar.disease) NOT LIKE '%healthy%'")
    long countDiseasedPlantsByUserId(Long userId);
    
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.userId = ?1 ORDER BY ar.analysisDate DESC LIMIT 10")
    List<AnalysisResult> findTop10ByUserIdOrderByAnalysisDateDesc(Long userId);
} 