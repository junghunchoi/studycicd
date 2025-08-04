package junghun.studycicd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * SLI (Service Level Indicator) / SLO (Service Level Objective) 평가 서비스
 * 실무에서 사용되는 카나리 배포 품질 평가 시스템
 */
@Service
public class SliSloEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(SliSloEvaluator.class);
    
    @Value("${prometheus.url:http://prometheus:9090}")
    private String prometheusUrl;
    
    // SLO 임계값 설정 (실무에서는 환경별로 다르게 설정)
    @Value("${slo.error-rate.threshold:2.0}")
    private Double errorRateThreshold;
    
    @Value("${slo.response-time.p95.threshold:1.0}")
    private Double responseTimeP95Threshold;
    
    @Value("${slo.response-time.p99.threshold:2.0}")
    private Double responseTimeP99Threshold;
    
    @Value("${slo.availability.threshold:99.9}")
    private Double availabilityThreshold;
    
    @Value("${slo.throughput.min-rps:1.0}")
    private Double minThroughputRps;
    
    private final WebClient webClient;
    
    public SliSloEvaluator() {
        WebClient client;
        try {
            client = WebClient.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                    .build();
        } catch (Exception e) {
            logger.warn("Failed to initialize WebClient, creating default client", e);
            client = WebClient.create();
        }
        this.webClient = client;
    }
    
    /**
     * 현재 상태의 SLI/SLO 평가
     */
    public SliSloResult evaluateCurrentState() {
        return evaluateCurrentState("2m");
    }
    
    /**
     * 지정된 시간 범위에서 SLI/SLO 평가
     */
    public SliSloResult evaluateCurrentState(String timeRange) {
        try {
            logger.debug("Evaluating SLI/SLO for time range: {}", timeRange);
            
            // 현재 메트릭 수집
            Double errorRate = getErrorRatePercent(timeRange);
            Double responseTimeP95 = getResponseTimePercentile(95, timeRange);
            Double responseTimeP99 = getResponseTimePercentile(99, timeRange);
            Double availability = getAvailability(timeRange);
            Double throughput = getThroughputRps(timeRange);
            Integer sampleSize = getSampleSize(timeRange);
            
            // 비즈니스 메트릭 수집
            Double canaryConversionRate = getCanaryConversionRate(timeRange);
            Double canaryErrorRate = getCanarySpecificErrorRate(timeRange);
            
            // SLO 준수 여부 평가
            boolean sloCompliant = evaluateSloCompliance(
                errorRate, responseTimeP95, responseTimeP99, availability, throughput
            );
            
            var result = new SliSloResult(
                errorRate, responseTimeP95, responseTimeP99, availability, throughput,
                canaryConversionRate, canaryErrorRate, sampleSize, sloCompliant,
                errorRateThreshold, responseTimeP95Threshold, responseTimeP99Threshold,
                availabilityThreshold, minThroughputRps
            );
            
            logger.debug("SLI/SLO evaluation result: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to evaluate SLI/SLO", e);
            return createFailsafeResult();
        }
    }
    
    /**
     * 에러율 계산 (2분간)
     */
    private Double getErrorRatePercent(String timeRange) {
        String query = String.format(
            "(rate(nginx_http_requests_total{status=~\"5..\"}[%s]) / " +
            "rate(nginx_http_requests_total[%s])) * 100", 
            timeRange, timeRange
        );
        return executePrometheusQuery(query);
    }
    
    /**
     * 응답시간 백분위수 계산
     */
    private Double getResponseTimePercentile(int percentile, String timeRange) {
        String query = String.format(
            "histogram_quantile(%.2f, rate(nginx_http_request_duration_seconds_bucket[%s]))",
            percentile / 100.0, timeRange
        );
        return executePrometheusQuery(query);
    }
    
    /**
     * 가용성 계산 (HTTP 200-399 응답 비율)
     */
    private Double getAvailability(String timeRange) {
        String query = String.format(
            "(rate(nginx_http_requests_total{status=~\"[23]..\"}[%s]) / " +
            "rate(nginx_http_requests_total[%s])) * 100",
            timeRange, timeRange
        );
        return executePrometheusQuery(query);
    }
    
    /**
     * 처리량 (RPS) 계산
     */
    private Double getThroughputRps(String timeRange) {
        String query = String.format("rate(nginx_http_requests_total[%s])", timeRange);
        return executePrometheusQuery(query);
    }
    
    /**
     * 샘플 수 계산
     */
    private Integer getSampleSize(String timeRange) {
        String query = String.format("increase(nginx_http_requests_total[%s])", timeRange);
        Double result = executePrometheusQuery(query);
        return result != null ? result.intValue() : 0;
    }
    
    /**
     * 카나리 버전 전환율 (비즈니스 메트릭 예시)
     */
    private Double getCanaryConversionRate(String timeRange) {
        try {
            // 실제로는 비즈니스 로직에 따라 구현
            // 예: 카나리 버전에서의 성공적인 주문/가입 비율
            String query = String.format(
                "rate(business_conversion_total{version=\"refactored\"}[%s]) / " +
                "rate(http_requests_total{version=\"refactored\"}[%s]) * 100",
                timeRange, timeRange
            );
            return executePrometheusQuery(query);
        } catch (Exception e) {
            // 비즈니스 메트릭이 없는 경우 null 반환
            return null;
        }
    }
    
    /**
     * 카나리 버전 특정 에러율
     */
    private Double getCanarySpecificErrorRate(String timeRange) {
        try {
            String query = String.format(
                "(rate(spring_http_server_requests_seconds_count{status=~\"5..\",version=\"refactored\"}[%s]) / " +
                "rate(spring_http_server_requests_seconds_count{version=\"refactored\"}[%s])) * 100",
                timeRange, timeRange
            );
            return executePrometheusQuery(query);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * SLO 준수 여부 평가
     */
    private boolean evaluateSloCompliance(Double errorRate, Double responseTimeP95, 
                                         Double responseTimeP99, Double availability, 
                                         Double throughput) {
        // 에러율 체크
        if (errorRate != null && errorRate > errorRateThreshold) {
            logger.warn("SLO violation: Error rate {:.2f}% exceeds threshold {:.2f}%", 
                       errorRate, errorRateThreshold);
            return false;
        }
        
        // 응답시간 P95 체크
        if (responseTimeP95 != null && responseTimeP95 > responseTimeP95Threshold) {
            logger.warn("SLO violation: Response time P95 {:.2f}s exceeds threshold {:.2f}s", 
                       responseTimeP95, responseTimeP95Threshold);
            return false;
        }
        
        // 응답시간 P99 체크
        if (responseTimeP99 != null && responseTimeP99 > responseTimeP99Threshold) {
            logger.warn("SLO violation: Response time P99 {:.2f}s exceeds threshold {:.2f}s", 
                       responseTimeP99, responseTimeP99Threshold);
            return false;
        }
        
        // 가용성 체크
        if (availability != null && availability < availabilityThreshold) {
            logger.warn("SLO violation: Availability {:.2f}% below threshold {:.2f}%", 
                       availability, availabilityThreshold);
            return false;
        }
        
        // 처리량 체크
        if (throughput != null && throughput < minThroughputRps) {
            logger.warn("SLO violation: Throughput {:.2f} RPS below minimum {:.2f} RPS", 
                       throughput, minThroughputRps);
            return false;
        }
        
        return true;
    }
    
    /**
     * Prometheus 쿼리 실행
     */
    private Double executePrometheusQuery(String query) {
        try {
            String url = prometheusUrl + "/api/v1/query?query=" + query;
            
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (response != null && "success".equals(response.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("result") instanceof List) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
                    if (!results.isEmpty()) {
                        Map<String, Object> result = results.get(0);
                        List<Object> value = (List<Object>) result.get("value");
                        if (value != null && value.size() > 1) {
                            String valueStr = value.get(1).toString();
                            return "NaN".equals(valueStr) ? null : Double.parseDouble(valueStr);
                        }
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.debug("Failed to execute Prometheus query: {}", query, e);
            return null;
        }
    }
    
    /**
     * 실패시 안전한 결과 반환
     */
    private SliSloResult createFailsafeResult() {
        logger.warn("Creating failsafe SLI/SLO result - considering deployment unsafe");
        return new SliSloResult(
            100.0, 10.0, 10.0, 0.0, 0.0, // 최악의 메트릭 값
            null, null, 0, false, // 비즈니스 메트릭과 SLO 실패
            errorRateThreshold, responseTimeP95Threshold, responseTimeP99Threshold,
            availabilityThreshold, minThroughputRps
        );
    }
    
    /**
     * SLI/SLO 평가 결과 클래스
     */
    public static class SliSloResult {
        // SLI 값들
        private final Double errorRatePercent;
        private final Double responseTimeP95;
        private final Double responseTimeP99;
        private final Double availability;
        private final Double throughputRps;
        private final Double canaryConversionRate;
        private final Double canaryErrorRate;
        private final Integer sampleSize;
        
        // SLO 평가 결과
        private final Boolean sloCompliant;
        
        // SLO 임계값들
        private final Double errorRateThreshold;
        private final Double responseTimeThreshold;
        private final Double responseTimeP99Threshold;
        private final Double availabilityThreshold;
        private final Double throughputThreshold;
        
        public SliSloResult(Double errorRatePercent, Double responseTimeP95, Double responseTimeP99,
                           Double availability, Double throughputRps, Double canaryConversionRate,
                           Double canaryErrorRate, Integer sampleSize, Boolean sloCompliant,
                           Double errorRateThreshold, Double responseTimeThreshold,
                           Double responseTimeP99Threshold, Double availabilityThreshold,
                           Double throughputThreshold) {
            this.errorRatePercent = errorRatePercent;
            this.responseTimeP95 = responseTimeP95;
            this.responseTimeP99 = responseTimeP99;
            this.availability = availability;
            this.throughputRps = throughputRps;
            this.canaryConversionRate = canaryConversionRate;
            this.canaryErrorRate = canaryErrorRate;
            this.sampleSize = sampleSize;
            this.sloCompliant = sloCompliant;
            this.errorRateThreshold = errorRateThreshold;
            this.responseTimeThreshold = responseTimeThreshold;
            this.responseTimeP99Threshold = responseTimeP99Threshold;
            this.availabilityThreshold = availabilityThreshold;
            this.throughputThreshold = throughputThreshold;
        }
        
        // Getters
        public Double getErrorRatePercent() { return errorRatePercent; }
        public Double getResponseTimeP95() { return responseTimeP95; }
        public Double getResponseTimeP99() { return responseTimeP99; }
        public Double getAvailability() { return availability; }
        public Double getThroughputRps() { return throughputRps; }
        public Double getCanaryConversionRate() { return canaryConversionRate; }
        public Double getCanaryErrorRate() { return canaryErrorRate; }
        public Integer getSampleSize() { return sampleSize; }
        public Boolean isSloCompliant() { return sloCompliant; }
        public Double getErrorRateThreshold() { return errorRateThreshold; }
        public Double getResponseTimeThreshold() { return responseTimeThreshold; }
        public Double getResponseTimeP99Threshold() { return responseTimeP99Threshold; }
        public Double getAvailabilityThreshold() { return availabilityThreshold; }
        public Double getThroughputThreshold() { return throughputThreshold; }
        
        @Override
        public String toString() {
            return String.format(
                "SliSloResult{errorRate=%.2f%%, responseTimeP95=%.2fs, availability=%.2f%%, " +
                "throughput=%.2f RPS, samples=%d, sloCompliant=%s}",
                errorRatePercent, responseTimeP95, availability, throughputRps, sampleSize, sloCompliant
            );
        }
    }
}