package junghun.studycicd.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TrafficWeightRequest {
    
    @NotNull(message = "Legacy weight cannot be null")
    @Min(value = 0, message = "Legacy weight must be at least 0")
    @Max(value = 100, message = "Legacy weight must be at most 100")
    private Integer legacyWeight;
    
    @NotNull(message = "Refactored weight cannot be null")
    @Min(value = 0, message = "Refactored weight must be at least 0")
    @Max(value = 100, message = "Refactored weight must be at most 100")
    private Integer refactoredWeight;

    public TrafficWeightRequest() {}

    public TrafficWeightRequest(Integer legacyWeight, Integer refactoredWeight) {
        this.legacyWeight = legacyWeight;
        this.refactoredWeight = refactoredWeight;
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

    public boolean isValidWeightSum() {
        return legacyWeight != null && refactoredWeight != null && 
               legacyWeight + refactoredWeight == 100;
    }
}