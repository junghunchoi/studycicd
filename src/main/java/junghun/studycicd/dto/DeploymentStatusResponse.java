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

    public DeploymentStatusResponse() {}

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

    // Getters and Setters
    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Integer currentStage) {
        this.currentStage = currentStage;
    }

    public Integer getTotalStages() {
        return totalStages;
    }

    public void setTotalStages(Integer totalStages) {
        this.totalStages = totalStages;
    }

    public Integer getCurrentPercentage() {
        return currentPercentage;
    }

    public void setCurrentPercentage(Integer currentPercentage) {
        this.currentPercentage = currentPercentage;
    }

    public List<Integer> getAvailableStages() {
        return availableStages;
    }

    public void setAvailableStages(List<Integer> availableStages) {
        this.availableStages = availableStages;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRollbackCount() {
        return rollbackCount;
    }

    public void setRollbackCount(Integer rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    public Integer getMaxRollbacks() {
        return maxRollbacks;
    }

    public void setMaxRollbacks(Integer maxRollbacks) {
        this.maxRollbacks = maxRollbacks;
    }
}