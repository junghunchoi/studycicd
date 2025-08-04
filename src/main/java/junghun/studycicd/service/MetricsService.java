package junghun.studycicd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    @Value("${prometheus.url:http://prometheus:9090}")
    private String prometheusUrl;
    
    @Value("${metrics.error-rate.threshold:2.0}")
    private Double errorRateThreshold;
    
    @Value("${metrics.response-time.threshold:1.5}")
    private Double responseTimeThreshold;
    
    private final WebClient webClient;

    public MetricsService() {
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

    public boolean isDeploymentSafe() {
        try {
            // Check error rate
            Double errorRate = getErrorRate();
            if (errorRate != null && errorRate > errorRateThreshold) {
                logger.warn("Error rate {}% exceeds threshold {}%", errorRate, errorRateThreshold);
                return false;
            }
            
            // Check response time
            Double responseTime = getAverageResponseTime();
            if (responseTime != null && responseTime > responseTimeThreshold) {
                logger.warn("Response time {}s exceeds threshold {}s", responseTime, responseTimeThreshold);
                return false;
            }
            
            logger.debug("Metrics check passed - Error rate: {}%, Response time: {}s", 
                        errorRate, responseTime);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to check metrics, considering deployment unsafe", e);
            return false;
        }
    }

    public Double getErrorRate() {
        try {
            String query = "rate(nginx_http_requests_total{status=~\"5..\"}[2m]) / rate(nginx_http_requests_total[2m]) * 100";
            return executePrometheusQuery(query);
        } catch (Exception e) {
            logger.error("Failed to get error rate", e);
            return null;
        }
    }

    public Double getAverageResponseTime() {
        try {
            String query = "rate(nginx_http_request_duration_seconds_sum[2m]) / rate(nginx_http_request_duration_seconds_count[2m])";
            return executePrometheusQuery(query);
        } catch (Exception e) {
            logger.error("Failed to get average response time", e);
            return null;
        }
    }

    public Map<String, Object> getCurrentMetrics() {
        try {
            Double errorRate = getErrorRate();
            Double responseTime = getAverageResponseTime();
            
            return Map.of(
                "errorRate", errorRate != null ? errorRate : 0.0,
                "averageResponseTime", responseTime != null ? responseTime : 0.0,
                "errorRateThreshold", errorRateThreshold,
                "responseTimeThreshold", responseTimeThreshold,
                "isHealthy", isDeploymentSafe(),
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            logger.error("Failed to get current metrics", e);
            return Map.of(
                "error", "Failed to retrieve metrics",
                "timestamp", System.currentTimeMillis()
            );
        }
    }

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
                if (data != null && data.get("result") instanceof java.util.List) {
                    java.util.List<Map<String, Object>> results = (java.util.List<Map<String, Object>>) data.get("result");
                    if (!results.isEmpty()) {
                        Map<String, Object> result = results.get(0);
                        java.util.List<Object> value = (java.util.List<Object>) result.get("value");
                        if (value != null && value.size() > 1) {
                            return Double.parseDouble(value.get(1).toString());
                        }
                    }
                }
            }
            
            return null;
            
        } catch (WebClientResponseException e) {
            logger.error("Prometheus query failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to execute Prometheus query", e);
            throw e;
        }
    }
}