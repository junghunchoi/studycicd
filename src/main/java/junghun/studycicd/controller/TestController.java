package junghun.studycicd.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
public class TestController {

    private final Counter requestCounter;
    private final Timer responseTimer;
    private final Random random = new Random();
    
    @Value("${info.app.version.type:default}")
    private String versionType;

    public TestController(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .register(meterRegistry);
        
        this.responseTimer = Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration")
                .register(meterRegistry);
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() throws Exception {
        return responseTimer.recordCallable(() -> {
            requestCounter.increment();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hello from " + versionType + " version!");
            response.put("timestamp", LocalDateTime.now());
            response.put("version", versionType);
            response.put("instance", System.getProperty("HOSTNAME", "unknown"));
            
            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() throws Exception {
        return responseTimer.recordCallable(() -> {
            requestCounter.increment();
            
            // 시뮬레이션을 위한 랜덤 지연
            try {
                Thread.sleep(random.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("version", versionType);
            response.put("timestamp", LocalDateTime.now());
            response.put("processingTime", random.nextInt(100) + "ms");
            
            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/error-simulation")
    public ResponseEntity<Map<String, Object>> errorSimulation() {
        requestCounter.increment();
        
        // 리팩토링 버전에서는 의도적으로 더 높은 에러율 시뮬레이션
        int errorRate = "refactored".equals(versionType) ? 10 : 5; // 10% vs 5%
        
        if (random.nextInt(100) < errorRate) {
            throw new RuntimeException("Simulated error for testing");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("version", versionType);
        response.put("message", "No error occurred");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        requestCounter.increment();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", versionType);
        response.put("timestamp", LocalDateTime.now());
        response.put("uptime", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}