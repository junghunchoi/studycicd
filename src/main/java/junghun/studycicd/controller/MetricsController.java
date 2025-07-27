package junghun.studycicd.controller;

import junghun.studycicd.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        Map<String, Object> metrics = metricsService.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        boolean isHealthy = metricsService.isDeploymentSafe();
        Double errorRate = metricsService.getErrorRate();
        Double responseTime = metricsService.getAverageResponseTime();
        
        Map<String, Object> healthStatus = Map.of(
            "isHealthy", isHealthy,
            "errorRate", errorRate != null ? errorRate : 0.0,
            "averageResponseTime", responseTime != null ? responseTime : 0.0,
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(healthStatus);
    }
}