package junghun.studycicd.service;

import junghun.studycicd.dto.TrafficWeightResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class TrafficManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrafficManagementService.class);
    
    private final NginxConfigService nginxConfigService;
    
    private Integer currentLegacyWeight = 95;
    private Integer currentRefactoredWeight = 5;
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public TrafficManagementService(NginxConfigService nginxConfigService) {
        this.nginxConfigService = nginxConfigService;
    }

    public TrafficWeightResponse getCurrentWeights() {
        return new TrafficWeightResponse(
            currentLegacyWeight, 
            currentRefactoredWeight, 
            "SUCCESS", 
            "Current traffic weights retrieved successfully"
        );
    }

    public TrafficWeightResponse updateWeights(Integer legacyWeight, Integer refactoredWeight) {
        if (legacyWeight + refactoredWeight != 100) {
            return new TrafficWeightResponse(
                currentLegacyWeight, 
                currentRefactoredWeight, 
                "ERROR", 
                "Traffic weights must sum to 100"
            );
        }

        try {
            boolean success = nginxConfigService.updateNginxConfig(legacyWeight, refactoredWeight);
            
            if (success) {
                this.currentLegacyWeight = legacyWeight;
                this.currentRefactoredWeight = refactoredWeight;
                this.lastUpdated = LocalDateTime.now();
                
                logger.info("Traffic weights updated successfully: Legacy={}%, Refactored={}%", 
                           legacyWeight, refactoredWeight);
                
                return new TrafficWeightResponse(
                    legacyWeight, 
                    refactoredWeight, 
                    "SUCCESS", 
                    "Traffic weights updated successfully"
                );
            } else {
                return new TrafficWeightResponse(
                    currentLegacyWeight, 
                    currentRefactoredWeight, 
                    "ERROR", 
                    "Failed to update NGINX configuration"
                );
            }
            
        } catch (Exception e) {
            logger.error("Failed to update traffic weights", e);
            return new TrafficWeightResponse(
                currentLegacyWeight, 
                currentRefactoredWeight, 
                "ERROR", 
                "Failed to update traffic weights: " + e.getMessage()
            );
        }
    }

    public Integer getCurrentLegacyWeight() {
        return currentLegacyWeight;
    }

    public Integer getCurrentRefactoredWeight() {
        return currentRefactoredWeight;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}