package junghun.studycicd.controller;

import junghun.studycicd.dto.TrafficWeightRequest;
import junghun.studycicd.dto.TrafficWeightResponse;
import junghun.studycicd.dto.DeploymentStatusResponse;
import junghun.studycicd.service.TrafficManagementService;
import junghun.studycicd.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final TrafficManagementService trafficManagementService;
    private final DeploymentService deploymentService;

    public TrafficController(TrafficManagementService trafficManagementService, 
                           DeploymentService deploymentService) {
        this.trafficManagementService = trafficManagementService;
        this.deploymentService = deploymentService;
    }

    @GetMapping("/status")
    public ResponseEntity<TrafficWeightResponse> getCurrentTrafficStatus() {
        TrafficWeightResponse response = trafficManagementService.getCurrentWeights();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/adjust")
    public ResponseEntity<TrafficWeightResponse> adjustTrafficWeights(
            @Valid @RequestBody TrafficWeightRequest request) {
        
        if (!request.isValidWeightSum()) {
            throw new IllegalArgumentException("Traffic weights must sum to 100");
        }
        
        TrafficWeightResponse response = trafficManagementService.updateWeights(
            request.getLegacyWeight(), 
            request.getRefactoredWeight()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/canary/start")
    public ResponseEntity<DeploymentStatusResponse> startCanaryDeployment() {
        DeploymentStatusResponse response = deploymentService.startCanaryDeployment();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/canary/next-stage")
    public ResponseEntity<DeploymentStatusResponse> proceedToNextStage() {
        DeploymentStatusResponse response = deploymentService.proceedToNextStage();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/canary/rollback")
    public ResponseEntity<DeploymentStatusResponse> rollbackDeployment() {
        DeploymentStatusResponse response = deploymentService.rollbackDeployment();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/canary/status")
    public ResponseEntity<DeploymentStatusResponse> getCanaryStatus() {
        DeploymentStatusResponse response = deploymentService.getDeploymentStatus();
        return ResponseEntity.ok(response);
    }
}