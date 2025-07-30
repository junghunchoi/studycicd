package junghun.studycicd.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.UUID;

@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor());
    }

    public static class RequestLoggingInterceptor implements HandlerInterceptor {
        private static final Logger logger = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

        @Value("${spring.application.name:studycicd}")
        private String applicationName;

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            long startTime = System.currentTimeMillis();
            
            // Request ID 설정 (NGINX에서 오거나 새로 생성)
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }
            
            // MDC에 추적 정보 설정
            MDC.put("requestId", requestId);
            MDC.put("upstream", request.getServerName() + ":" + request.getServerPort());
            
            // 요청 정보 로깅
            String canaryWeight = request.getHeader("X-Canary-Weight");
            String legacyWeight = request.getHeader("X-Legacy-Weight");
            
            logger.info("REQUEST_START|{}|{}|{}|{}|canary_weight={}|legacy_weight={}|user_agent={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    requestId,
                    canaryWeight != null ? canaryWeight : "unknown",
                    legacyWeight != null ? legacyWeight : "unknown",
                    request.getHeader("User-Agent"));
            
            request.setAttribute("startTime", startTime);
            request.setAttribute("requestId", requestId);
            
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            try {
                long startTime = (Long) request.getAttribute("startTime");
                long duration = System.currentTimeMillis() - startTime;
                String requestId = (String) request.getAttribute("requestId");
                
                logger.info("REQUEST_END|{}|{}|{}|{}|duration={}ms|status={}|response_size={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getRemoteAddr(),
                        requestId,
                        duration,
                        response.getStatus(),
                        response.getHeaderNames().contains("Content-Length") ? 
                            response.getHeader("Content-Length") : "unknown");
                
                if (ex != null) {
                    logger.error("REQUEST_ERROR|{}|{}|{}|error={}", 
                            request.getMethod(),
                            request.getRequestURI(),
                            requestId,
                            ex.getMessage(), ex);
                }
            } finally {
                MDC.clear();
            }
        }
    }
}