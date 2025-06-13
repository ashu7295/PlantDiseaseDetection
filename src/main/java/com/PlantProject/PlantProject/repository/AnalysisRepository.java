package com.PlantProject.PlantProject.repository;

import com.PlantProject.PlantProject.model.Analysis;
import com.PlantProject.PlantProject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByUserOrderByCreatedAtDesc(User user);
    
    List<Analysis> findTop6ByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT COUNT(a) FROM Analysis a WHERE a.user = ?1 AND a.disease LIKE '%healthy%'")
    Long countHealthyPlants(User user);
    
    @Query("SELECT COUNT(a) FROM Analysis a WHERE a.user = ?1 AND a.disease NOT LIKE '%healthy%'")
    Long countDiseasedPlants(User user);
    
    @Query("SELECT COUNT(a) FROM Analysis a WHERE a.user = ?1")
    Long countTotalAnalyses(User user);

    List<Analysis> findByUser(User user);
    long countByUser(User user);
    long countByUserAndDiseaseContaining(User user, String disease);
    long countByUserAndDiseaseNotContaining(User user, String disease);
} 