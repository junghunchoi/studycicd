package junghun.studycicd.service;

import junghun.studycicd.dto.DeploymentStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);
    private static final List<Integer> DEPLOYMENT_STAGES = Arrays.asList(5, 10, 25, 50, 100);
    private static final Integer MAX_ROLLBACKS = 3;
    
    private final TrafficManagementService trafficManagementService;
    private final MetricsService metricsService;
    
    // Deployment state
    private String currentDeploymentId;
    private String deploymentStatus = "STABLE"; // STABLE, DEPLOYING, ROLLING_BACK, FAILED
    private Integer currentStage = 0;
    private Integer rollbackCount = 0;
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public DeploymentService(TrafficManagementService trafficManagementService, 
                           MetricsService metricsService) {
        this.trafficManagementService = trafficManagementService;
        this.metricsService = metricsService;
    }

    public DeploymentStatusResponse startCanaryDeployment() {
        if (!"STABLE".equals(deploymentStatus)) {
            return new DeploymentStatusResponse(
                currentDeploymentId, deploymentStatus, currentStage, 
                DEPLOYMENT_STAGES.size(), getCurrentPercentage(),
                DEPLOYMENT_STAGES, "Deployment already in progress", 
                rollbackCount, MAX_ROLLBACKS
            );
        }
        
        if (rollbackCount >= MAX_ROLLBACKS) {
            return new DeploymentStatusResponse(
                currentDeploymentId, "FAILED", currentStage, 
                DEPLOYMENT_STAGES.size(), getCurrentPercentage(),
                DEPLOYMENT_STAGES, "Maximum rollbacks exceeded - manual intervention required", 
                rollbackCount, MAX_ROLLBACKS
            );
        }

        // Start new deployment
        currentDeploymentId = "deploy-" + UUID.randomUUID().toString().substring(0, 8);
        deploymentStatus = "DEPLOYING";
        currentStage = 0;
        lastUpdated = LocalDateTime.now();
        
        // Set initial traffic to first stage
        Integer initialPercentage = DEPLOYMENT_STAGES.get(0);
        trafficManagementService.updateWeights(100 - initialPercentage, initialPercentage);
        
        logger.info("Started canary deployment: {} with {}% traffic to refactored version", 
                   currentDeploymentId, initialPercentage);
        
        return new DeploymentStatusResponse(
            currentDeploymentId, deploymentStatus, currentStage, 
            DEPLOYMENT_STAGES.size(), initialPercentage,
            DEPLOYMENT_STAGES, "Canary deployment started successfully", 
            rollbackCount, MAX_ROLLBACKS
        );
    }

    public DeploymentStatusResponse proceedToNextStage() {
        if (!"DEPLOYING".equals(deploymentStatus)) {
            return new DeploymentStatusResponse(
                currentDeploymentId, deploymentStatus, currentStage, 
                DEPLOYMENT_STAGES.size(), getCurrentPercentage(),
                DEPLOYMENT_STAGES, "No deployment in progress", 
                rollbackCount, MAX_ROLLBACKS
            );
        }
        
        // Check metrics before proceeding
        if (!metricsService.isDeploymentSafe()) {
            logger.warn("Metrics indicate unsafe deployment, triggering rollback");
            return rollbackDeployment();
        }
        
        // Move to next stage
        currentStage++;
        
        if (currentStage >= DEPLOYMENT_STAGES.size()) {
            // Deployment complete
            deploymentStatus = "STABLE";
            rollbackCount = 0; // Reset rollback count on successful deployment
            lastUpdated = LocalDateTime.now();
            
            logger.info("Canary deployment {} completed successfully", currentDeploymentId);
            
            return new DeploymentStatusResponse(
                currentDeploymentId, deploymentStatus, currentStage, 
                DEPLOYMENT_STAGES.size(), 100,
                DEPLOYMENT_STAGES, "Deployment completed successfully", 
                rollbackCount, MAX_ROLLBACKS
            );
        }
        
        // Update traffic for next stage
        Integer nextPercentage = DEPLOYMENT_STAGES.get(currentStage);
        trafficManagementService.updateWeights(100 - nextPercentage, nextPercentage);
        lastUpdated = LocalDateTime.now();
        
        logger.info("Proceeded to deployment stage {}: {}% traffic to refactored version", 
                   currentStage + 1, nextPercentage);
        
        return new DeploymentStatusResponse(
            currentDeploymentId, deploymentStatus, currentStage, 
            DEPLOYMENT_STAGES.size(), nextPercentage,
            DEPLOYMENT_STAGES, "Proceeded to next deployment stage", 
            rollbackCount, MAX_ROLLBACKS
        );
    }

    public DeploymentStatusResponse rollbackDeployment() {
        if (rollbackCount >= MAX_ROLLBACKS) {
            deploymentStatus = "FAILED";
            return new DeploymentStatusResponse(
                currentDeploymentId, deploymentStatus, currentStage, 
                DEPLOYMENT_STAGES.size(), getCurrentPercentage(),
                DEPLOYMENT_STAGES, "Maximum rollbacks exceeded - manual intervention required", 
                rollbackCount, MAX_ROLLBACKS
            );
        }
        
        // Rollback to 100% legacy traffic
        trafficManagementService.updateWeights(100, 0);
        
        deploymentStatus = "STABLE";
        currentStage = 0;
        rollbackCount++;
        lastUpdated = LocalDateTime.now();
        
        logger.warn("Deployment {} rolled back. Rollback count: {}", 
                   currentDeploymentId, rollbackCount);
        
        return new DeploymentStatusResponse(
            currentDeploymentId, deploymentStatus, currentStage, 
            DEPLOYMENT_STAGES.size(), 0,
            DEPLOYMENT_STAGES, "Deployment rolled back successfully", 
            rollbackCount, MAX_ROLLBACKS
        );
    }

    public DeploymentStatusResponse getDeploymentStatus() {
        return new DeploymentStatusResponse(
            currentDeploymentId, deploymentStatus, currentStage, 
            DEPLOYMENT_STAGES.size(), getCurrentPercentage(),
            DEPLOYMENT_STAGES, "Current deployment status", 
            rollbackCount, MAX_ROLLBACKS
        );
    }
    
    private Integer getCurrentPercentage() {
        if (currentStage >= DEPLOYMENT_STAGES.size()) {
            return 100;
        }
        if (currentStage < 0) {
            return 0;
        }
        return DEPLOYMENT_STAGES.get(currentStage);
    }
}