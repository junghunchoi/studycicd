package junghun.studycicd.controller;

import junghun.studycicd.service.AutoDeploymentScheduler;
import junghun.studycicd.service.SliSloEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 자동 카나리 배포 컨트롤러
 * 실무에서 사용되는 자동화된 배포 관리 API
 */
@RestController
@RequestMapping("/api/auto-deployment")
public class AutoDeploymentController {
    
    private final AutoDeploymentScheduler autoDeploymentScheduler;
    private final SliSloEvaluator sliSloEvaluator;
    
    public AutoDeploymentController(AutoDeploymentScheduler autoDeploymentScheduler,
                                   SliSloEvaluator sliSloEvaluator) {
        this.autoDeploymentScheduler = autoDeploymentScheduler;
        this.sliSloEvaluator = sliSloEvaluator;
    }
    
    /**
     * 자동 카나리 배포 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAutoDeployment() {
        boolean started = autoDeploymentScheduler.startAutoDeployment();
        
        if (started) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Auto deployment started successfully",
                "status", autoDeploymentScheduler.getAutoDeploymentStatus()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to start auto deployment",
                "status", autoDeploymentScheduler.getAutoDeploymentStatus()
            ));
        }
    }
    
    /**
     * 자동 카나리 배포 중단
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopAutoDeployment() {
        autoDeploymentScheduler.stopAutoDeployment();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Auto deployment stopped",
            "status", autoDeploymentScheduler.getAutoDeploymentStatus()
        ));
    }
    
    /**
     * 자동 배포 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<AutoDeploymentScheduler.AutoDeploymentStatus> getAutoDeploymentStatus() {
        return ResponseEntity.ok(autoDeploymentScheduler.getAutoDeploymentStatus());
    }
    
    /**
     * 현재 SLI/SLO 메트릭 조회
     */
    @GetMapping("/sli-slo")
    public ResponseEntity<SliSloEvaluator.SliSloResult> getCurrentSliSlo() {
        return ResponseEntity.ok(sliSloEvaluator.evaluateCurrentState());
    }
    
    /**
     * 지정된 시간 범위의 SLI/SLO 메트릭 조회
     */
    @GetMapping("/sli-slo/{timeRange}")
    public ResponseEntity<SliSloEvaluator.SliSloResult> getSliSloForTimeRange(
            @PathVariable String timeRange) {
        return ResponseEntity.ok(sliSloEvaluator.evaluateCurrentState(timeRange));
    }
    
    /**
     * SLI/SLO 임계값 및 설정 정보 조회
     */
    @GetMapping("/sli-slo/config")
    public ResponseEntity<Map<String, Object>> getSliSloConfig() {
        var currentResult = sliSloEvaluator.evaluateCurrentState();
        
        return ResponseEntity.ok(Map.of(
            "thresholds", Map.of(
                "errorRatePercent", currentResult.getErrorRateThreshold(),
                "responseTimeP95Seconds", currentResult.getResponseTimeThreshold(),
                "responseTimeP99Seconds", currentResult.getResponseTimeP99Threshold(),
                "availabilityPercent", currentResult.getAvailabilityThreshold(),
                "throughputRps", currentResult.getThroughputThreshold()
            ),
            "description", Map.of(
                "errorRatePercent", "Maximum allowed error rate percentage",
                "responseTimeP95Seconds", "Maximum allowed 95th percentile response time",
                "responseTimeP99Seconds", "Maximum allowed 99th percentile response time", 
                "availabilityPercent", "Minimum required availability percentage",
                "throughputRps", "Minimum required requests per second"
            )
        ));
    }
    
    /**
     * 종합 대시보드 정보 (자동 배포 + SLI/SLO)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        var autoDeploymentStatus = autoDeploymentScheduler.getAutoDeploymentStatus();
        var sliSloResult = sliSloEvaluator.evaluateCurrentState();
        
        return ResponseEntity.ok(Map.of(
            "autoDeployment", autoDeploymentStatus,
            "sliSlo", sliSloResult,
            "summary", Map.of(
                "deploymentInProgress", autoDeploymentStatus.getInProgress(),
                "sloCompliant", sliSloResult.isSloCompliant(),
                "currentErrorRate", sliSloResult.getErrorRatePercent(),
                "currentResponseTime", sliSloResult.getResponseTimeP95(),
                "sampleSize", sliSloResult.getSampleSize()
            ),
            "timestamp", System.currentTimeMillis()
        ));
    }
}