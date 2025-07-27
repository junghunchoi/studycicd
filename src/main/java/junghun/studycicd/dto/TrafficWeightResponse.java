package junghun.studycicd.dto;

import java.time.LocalDateTime;

public class TrafficWeightResponse {
    private Integer legacyWeight;
    private Integer refactoredWeight;
    private LocalDateTime lastUpdated;
    private String status;
    private String message;

    public TrafficWeightResponse() {}

    public TrafficWeightResponse(Integer legacyWeight, Integer refactoredWeight, 
                                String status, String message) {
        this.legacyWeight = legacyWeight;
        this.refactoredWeight = refactoredWeight;
        this.status = status;
        this.message = message;
        this.lastUpdated = LocalDateTime.now();
    }

    public Integer getLegacyWeight() {
        return legacyWeight;
    }

    public void setLegacyWeight(Integer legacyWeight) {
        this.legacyWeight = legacyWeight;
    }

    public Integer getRefactoredWeight() {
        return refactoredWeight;
    }

    public void setRefactoredWeight(Integer refactoredWeight) {
        this.refactoredWeight = refactoredWeight;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}