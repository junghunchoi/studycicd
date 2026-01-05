package junghun.studycicd.simulation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ErrorSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(ErrorSimulationService.class);
    private final Random random = new Random();
    private final ErrorSimulationConfig config;
    private final MeterRegistry meterRegistry;

    // 에러 통계
    private final AtomicLong totalErrorsSimulated = new AtomicLong(0);
    private final Map<ErrorType, AtomicLong> errorCountByType = new HashMap<>();
    private final Map<ErrorType, Counter> errorCounters = new HashMap<>();

    public ErrorSimulationService(ErrorSimulationConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        for (ErrorType errorType : ErrorType.values()) {
            errorCountByType.put(errorType, new AtomicLong(0));

            Counter counter = Counter.builder("simulated_errors_total")
                    .tag("error_type", errorType.name().toLowerCase())
                    .tag("http_status", String.valueOf(errorType.getStatus().value()))
                    .description("Total number of simulated errors by type")
                    .register(meterRegistry);

            errorCounters.put(errorType, counter);
        }
    }

    /**
     * API 호출 시 에러를 시뮬레이션
     */
    public void simulateApiError() throws SimulatedErrorException {
        if (!config.isApiErrorEnabled()) {
            return;
        }

        int errorRate = config.getApiErrorRate();
        if (random.nextInt(100) < errorRate) {
            throwRandomError("API");
        }
    }

    /**
     * 주기적 에러 발생
     */
    public void simulatePeriodicError() {
        if (!config.isPeriodicErrorEnabled()) {
            return;
        }

        int errorRate = config.getPeriodicErrorRate();
        if (random.nextInt(100) < errorRate) {
            try {
                throwRandomError("PERIODIC");
            } catch (SimulatedErrorException e) {
                // 주기적 에러는 예외를 던지지 않고 로깅만 수행
                logger.error("Periodic error simulation: {}", e.getMessage());
            }
        }
    }

    /**
     * 특정 타입의 에러 발생
     */
    public void simulateSpecificError(ErrorType errorType) throws SimulatedErrorException {
        throwError(errorType, "SPECIFIC");
    }

    /**
     * 랜덤 에러 발생
     */
    private void throwRandomError(String source) throws SimulatedErrorException {
        ErrorType errorType = selectWeightedErrorType();
        throwError(errorType, source);
    }

    /**
     * 가중치 기반 에러 타입 선택
     */
    private ErrorType selectWeightedErrorType() {
        int totalWeight = config.getTotalWeight();
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        if ((currentWeight += config.getInternalErrorWeight()) > randomValue) {
            return ErrorType.INTERNAL_SERVER_ERROR;
        }
        if ((currentWeight += config.getBadRequestWeight()) > randomValue) {
            return ErrorType.BAD_REQUEST;
        }
        if ((currentWeight += config.getNotFoundWeight()) > randomValue) {
            return ErrorType.NOT_FOUND;
        }
        if ((currentWeight += config.getServiceUnavailableWeight()) > randomValue) {
            return ErrorType.SERVICE_UNAVAILABLE;
        }
        if ((currentWeight += config.getTimeoutWeight()) > randomValue) {
            return ErrorType.TIMEOUT;
        }
        if ((currentWeight += config.getDatabaseErrorWeight()) > randomValue) {
            return ErrorType.DATABASE_ERROR;
        }
        if ((currentWeight += config.getExternalApiErrorWeight()) > randomValue) {
            return ErrorType.EXTERNAL_API_ERROR;
        }
        return ErrorType.MEMORY_ERROR;
    }

    /**
     * 에러를 발생시키고 로깅 및 메트릭 수집
     */
    private void throwError(ErrorType errorType, String source) throws SimulatedErrorException {
        String message = errorType.getRandomMessage();

        // 통계 업데이트
        totalErrorsSimulated.incrementAndGet();
        errorCountByType.get(errorType).incrementAndGet();
        errorCounters.get(errorType).increment();

        // 로그 레벨에 따라 다르게 로깅
        logError(errorType, message, source);

        // 예외 발생
        throw new SimulatedErrorException(errorType, message);
    }

    /**
     * 에러 타입에 따라 다른 로그 레벨로 로깅
     */
    private void logError(ErrorType errorType, String message, String source) {
        String logMessage = String.format("[%s] Simulated %s error: %s",
                source, errorType.name(), message);

        switch (errorType.getStatus().series()) {
            case CLIENT_ERROR:
                logger.warn(logMessage);
                break;
            case SERVER_ERROR:
                logger.error(logMessage);
                break;
            default:
                logger.info(logMessage);
        }

        // 추가 상세 로깅 (디버그 레벨)
        logger.debug("Error simulation details - Type: {}, Status: {}, Source: {}",
                errorType.name(), errorType.getStatus().value(), source);
    }

    /**
     * 통계 정보 조회
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrorsSimulated", totalErrorsSimulated.get());

        Map<String, Long> errorsByType = new HashMap<>();
        for (Map.Entry<ErrorType, AtomicLong> entry : errorCountByType.entrySet()) {
            errorsByType.put(entry.getKey().name(), entry.getValue().get());
        }
        stats.put("errorsByType", errorsByType);

        return stats;
    }

    /**
     * 통계 초기화
     */
    public void resetStatistics() {
        totalErrorsSimulated.set(0);
        for (AtomicLong count : errorCountByType.values()) {
            count.set(0);
        }
        logger.info("Error simulation statistics have been reset");
    }
}
