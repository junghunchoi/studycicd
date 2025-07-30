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

    /**
     * 현재 애플리케이션의 주요 메트릭을 조회합니다.
     * 이 메서드는 현재 오류율, 평균 응답 시간, 설정된 임계값 및 전반적인 상태를 포함하는 종합적인 메트릭 정보를 반환합니다.
     *
     * @return 현재 메트릭 상태를 담은 {@link ResponseEntity}. 주요 키는 다음과 같습니다:
     *         - "errorRate": 현재 오류율 (백분율)
     *         - "averageResponseTime": 평균 응답 시간 (초)
     *         - "errorRateThreshold": 설정된 오류율 임계값
     *         - "responseTimeThreshold": 설정된 응답 시간 임계값
     *         - "isHealthy": 현재 배포가 안전한지 여부
     *         - "timestamp": 메트릭 조회 시간 (밀리초)
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        Map<String, Object> metrics = metricsService.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * 현재 배포 상태의 건강 상태를 확인합니다.
     * 이 메서드는 오류율 및 응답 시간 메트릭을 기반으로 시스템이 안정적인지 여부를 평가하고,
     * 현재 메트릭 값과 함께 건강 상태를 반환합니다.
     *
     * @return 현재 건강 상태 정보를 담은 {@link ResponseEntity}. 주요 키는 다음과 같습니다:
     *         - "isHealthy": 현재 배포가 안전한지 여부 (true/false)
     *         - "errorRate": 현재 오류율 (백분율)
     *         - "averageResponseTime": 평균 응답 시간 (초)
     *         - "timestamp": 상태 확인 시간 (밀리초)
     */
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