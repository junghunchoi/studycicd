package junghun.studycicd.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MetricsInterceptor implements HandlerInterceptor {
    
    private final Counter totalRequestCounter;
    private final Counter successRequestCounter;
    private final Timer requestTimer;
    
    public MetricsInterceptor(MeterRegistry meterRegistry) {
        this.totalRequestCounter = Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .register(meterRegistry);
        
        this.successRequestCounter = Counter.builder("http_requests_total")
                .description("Total number of successful HTTP requests")
                .tag("status", "success")
                .register(meterRegistry);
        
        this.requestTimer = Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration in seconds")
                .register(meterRegistry);
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                            Object handler) throws Exception {
        
        if (shouldTrackRequest(request)) {
            request.setAttribute("startTime", System.nanoTime());
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        
        if (shouldTrackRequest(request)) {
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.nanoTime() - startTime;
                requestTimer.record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
            
            totalRequestCounter.increment();
            
            if (response.getStatus() < 400) {
                successRequestCounter.increment();
            }
        }
    }
    
    private boolean shouldTrackRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        return !path.equals("/health") && 
               !path.equals("/nginx_status") && 
               !path.startsWith("/actuator");
    }
}