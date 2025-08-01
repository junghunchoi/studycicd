package junghun.studycicd.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DeploymentStatusResponse {
    private String deploymentId;
    private String status; // STABLE, DEPLOYING, ROLLING_BACK, FAILED
    private Integer currentStage;
    private Integer totalStages;
    private Integer currentPercentage;
    private List<Integer> availableStages;
    private LocalDateTime lastUpdated;
    private String message;
    private Integer rollbackCount;
    private Integer maxRollbacks;


    public DeploymentStatusResponse(String deploymentId, String status, Integer currentStage, 
                                   Integer totalStages, Integer currentPercentage, 
                                   List<Integer> availableStages, String message,
                                   Integer rollbackCount, Integer maxRollbacks) {
        this.deploymentId = deploymentId;
        this.status = status;
        this.currentStage = currentStage;
        this.totalStages = totalStages;
        this.currentPercentage = currentPercentage;
        this.availableStages = availableStages;
        this.message = message;
        this.rollbackCount = rollbackCount;
        this.maxRollbacks = maxRollbacks;
        this.lastUpdated = LocalDateTime.now();
    }
}