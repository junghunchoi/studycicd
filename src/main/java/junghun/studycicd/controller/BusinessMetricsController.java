package junghun.studycicd.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 비즈니스 메트릭 및 A/B 테스트 컨트롤러
 * 실무에서 사용되는 카나리 배포 시 비즈니스 영향 측정
 */
@RestController  
@RequestMapping("/api/business")
public class BusinessMetricsController {
    
    private final Counter orderAttempts;
    private final Counter orderSuccess; 
    private final Counter signupAttempts;
    private final Counter signupSuccess;
    private final Counter loginAttempts;
    private final Counter loginSuccess;
    private final Timer checkoutTime;
    private final Random random = new Random();
    
    @Value("${info.app.version.type:default}")
    private String versionType;
    
    public BusinessMetricsController(MeterRegistry meterRegistry) {
        // versionType이 null인 경우 기본값 설정
        String version = versionType != null ? versionType : "default";
        
        // 주문 관련 메트릭
        this.orderAttempts = Counter.builder("business_orders_attempted_total")
                .description("Total number of order attempts")
                .tag("version", version)
                .register(meterRegistry);
                
        this.orderSuccess = Counter.builder("business_orders_completed_total")
                .description("Total number of successful orders")
                .tag("version", version)
                .register(meterRegistry);
        
        // 회원가입 관련 메트릭        
        this.signupAttempts = Counter.builder("business_signups_attempted_total")
                .description("Total number of signup attempts")
                .tag("version", version)
                .register(meterRegistry);
                
        this.signupSuccess = Counter.builder("business_signups_completed_total")
                .description("Total number of successful signups")
                .tag("version", version)
                .register(meterRegistry);
        
        // 로그인 관련 메트릭
        this.loginAttempts = Counter.builder("business_logins_attempted_total")
                .description("Total number of login attempts")
                .tag("version", version)
                .register(meterRegistry);
                
        this.loginSuccess = Counter.builder("business_logins_completed_total")
                .description("Total number of successful logins")  
                .tag("version", version)
                .register(meterRegistry);
        
        // 체크아웃 시간 메트릭
        this.checkoutTime = Timer.builder("business_checkout_duration_seconds")
                .description("Time taken for checkout process")
                .tag("version", version)
                .register(meterRegistry);
    }
    
    /**
     * 주문 시뮬레이션 엔드포인트
     */
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> simulateOrder(@RequestBody Map<String, Object> orderData) throws Exception {
        return checkoutTime.recordCallable(() -> {
            orderAttempts.increment();
            
            // 버전별 성공률 시뮬레이션 (리팩토링 버전이 더 나은 성능)
            String currentVersion = versionType != null ? versionType : "default";
            double successRate = "refactored".equals(currentVersion) ? 0.95 : 0.90;
            boolean isSuccess = random.nextDouble() < successRate;
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", UUID.randomUUID().toString());
            response.put("version", currentVersion);
            response.put("timestamp", LocalDateTime.now());
            response.put("success", isSuccess);
            
            if (isSuccess) {
                orderSuccess.increment();
                
                // 주문 성공 시 추가 정보
                response.put("amount", orderData.getOrDefault("amount", 100.0));
                response.put("processingTime", random.nextInt(1000) + 500); // 500-1500ms
                response.put("message", "Order processed successfully");
            } else {
                response.put("error", "Order processing failed");
                response.put("errorCode", "ORDER_" + random.nextInt(3) + 1);
            }
            
            return ResponseEntity.ok(response);
        });
    }
    
    /**
     * 회원가입 시뮬레이션 엔드포인트
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> simulateSignup(@RequestBody Map<String, Object> signupData) {
        signupAttempts.increment();
        
        // 리팩토링 버전에서 더 나은 회원가입 경험 시뮬레이션
        String currentVersion = versionType != null ? versionType : "default";
        double successRate = "refactored".equals(currentVersion) ? 0.92 : 0.85;
        boolean isSuccess = random.nextDouble() < successRate;
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", currentVersion);
        response.put("timestamp", LocalDateTime.now());
        response.put("success", isSuccess);
        
        if (isSuccess) {
            signupSuccess.increment();
            response.put("userId", UUID.randomUUID().toString());
            response.put("email", signupData.getOrDefault("email", "user@example.com"));
            response.put("message", "Signup completed successfully");
        } else {
            response.put("error", "Signup failed");
            response.put("errorCode", "SIGNUP_" + random.nextInt(3) + 1);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 로그인 시뮬레이션 엔드포인트
     */
    @PostMapping("/login")  
    public ResponseEntity<Map<String, Object>> simulateLogin(@RequestBody Map<String, Object> loginData) {
        loginAttempts.increment();
        
        // 기본 성공률 (버전별 차이 최소)
        String currentVersion = versionType != null ? versionType : "default";
        double successRate = 0.88;
        boolean isSuccess = random.nextDouble() < successRate;
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", currentVersion);
        response.put("timestamp", LocalDateTime.now());
        response.put("success", isSuccess);
        
        if (isSuccess) {
            loginSuccess.increment();
            response.put("sessionId", UUID.randomUUID().toString());
            response.put("username", loginData.getOrDefault("username", "testuser"));
            response.put("message", "Login successful");
        } else {
            response.put("error", "Login failed");
            response.put("errorCode", "AUTH_" + random.nextInt(3) + 1);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * A/B 테스트용 기능 토글 엔드포인트
     */
    @GetMapping("/feature/{featureName}")
    public ResponseEntity<Map<String, Object>> getFeatureFlag(@PathVariable String featureName,
                                                             @RequestParam(defaultValue = "default") String userGroup) {
        String currentVersion = versionType != null ? versionType : "default";
        Map<String, Object> response = new HashMap<>();
        response.put("feature", featureName);
        response.put("version", currentVersion);
        response.put("userGroup", userGroup);
        response.put("timestamp", LocalDateTime.now());
        
        // 버전 및 사용자 그룹별 기능 플래그 시뮬레이션
        boolean featureEnabled = determineFeatureFlag(featureName, currentVersion, userGroup);
        response.put("enabled", featureEnabled);
        
        // 기능별 추가 설정
        if ("checkout_optimization".equals(featureName)) {
            response.put("config", Map.of(
                "newUI", featureEnabled && "refactored".equals(currentVersion),
                "expressCheckout", featureEnabled,
                "recommendedItems", featureEnabled ? 3 : 1
            ));
        } else if ("personalization".equals(featureName)) {
            response.put("config", Map.of(
                "personalizedRecommendations", featureEnabled,
                "customDashboard", featureEnabled && "premium".equals(userGroup),
                "aiSuggestions", featureEnabled && "refactored".equals(currentVersion)
            ));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 비즈니스 메트릭 요약 조회
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getBusinessMetricsSummary() {
        String currentVersion = versionType != null ? versionType : "default";
        Map<String, Object> summary = new HashMap<>();
        
        // 현재 버전의 메트릭 (실제로는 Prometheus에서 쿼리)
        summary.put("version", currentVersion);
        summary.put("orderConversionRate", calculateConversionRate(orderSuccess.count(), orderAttempts.count()));
        summary.put("signupConversionRate", calculateConversionRate(signupSuccess.count(), signupAttempts.count()));  
        summary.put("loginSuccessRate", calculateConversionRate(loginSuccess.count(), loginAttempts.count()));
        
        summary.put("totalOrders", orderAttempts.count());
        summary.put("successfulOrders", orderSuccess.count());
        summary.put("totalSignups", signupAttempts.count());
        summary.put("successfulSignups", signupSuccess.count());
        
        summary.put("timestamp", LocalDateTime.now());
        summary.put("date", LocalDate.now());
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * A/B 테스트 결과 조회
     */
    @GetMapping("/ab-test/{testName}")
    public ResponseEntity<Map<String, Object>> getAbTestResults(@PathVariable String testName) {
        Map<String, Object> results = new HashMap<>();
        
        // 시뮬레이션된 A/B 테스트 결과
        results.put("testName", testName);
        results.put("status", "running");
        results.put("startDate", LocalDate.now().minusDays(7));
        results.put("endDate", LocalDate.now().plusDays(7));
        
        // 버전별 결과 시뮬레이션
        Map<String, Object> legacyResults = new HashMap<>();
        legacyResults.put("participants", 1000);
        legacyResults.put("conversions", 85);
        legacyResults.put("conversionRate", 8.5);
        legacyResults.put("confidenceLevel", 95.0);
        
        Map<String, Object> refactoredResults = new HashMap<>();
        refactoredResults.put("participants", 200);
        refactoredResults.put("conversions", 19);
        refactoredResults.put("conversionRate", 9.5);
        refactoredResults.put("confidenceLevel", 85.0);
        
        results.put("variants", Map.of(
            "legacy", legacyResults,
            "refactored", refactoredResults
        ));
        
        // 통계적 유의성
        results.put("statisticalSignificance", Map.of(
            "isSignificant", false, // 아직 샘플 수 부족
            "pValue", 0.12,
            "recommendedAction", "continue_test"
        ));
        
        return ResponseEntity.ok(results);
    }
    
    private boolean determineFeatureFlag(String featureName, String version, String userGroup) {
        // 간단한 기능 플래그 로직
        switch (featureName) {
            case "checkout_optimization":
                return "refactored".equals(version) || "premium".equals(userGroup);
            case "personalization":
                return "refactored".equals(version) && random.nextDouble() > 0.3;
            case "new_dashboard":
                return "refactored".equals(version);
            default:
                return random.nextBoolean();
        }
    }
    
    private double calculateConversionRate(double successes, double attempts) {
        return attempts > 0 ? (successes / attempts) * 100 : 0.0;
    }
}