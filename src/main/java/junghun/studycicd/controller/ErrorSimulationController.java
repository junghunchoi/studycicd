package junghun.studycicd.controller;

import junghun.studycicd.simulation.ErrorSimulationConfig;
import junghun.studycicd.simulation.ErrorSimulationService;
import junghun.studycicd.simulation.ErrorType;
import junghun.studycicd.simulation.SimulatedErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/error-simulation")
public class ErrorSimulationController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorSimulationController.class);
    private final ErrorSimulationService errorSimulationService;
    private final ErrorSimulationConfig config;

    public ErrorSimulationController(ErrorSimulationService errorSimulationService,
                                      ErrorSimulationConfig config) {
        this.errorSimulationService = errorSimulationService;
        this.config = config;
    }

    /**
     * 랜덤 에러 발생 API
     */
    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> randomError() throws SimulatedErrorException {
        logger.info("Random error simulation endpoint called");
        errorSimulationService.simulateApiError();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "No error occurred this time");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 타입의 에러 발생 API
     */
    @GetMapping("/type/{errorType}")
    public ResponseEntity<Map<String, Object>> specificError(@PathVariable String errorType) throws SimulatedErrorException {
        logger.info("Specific error simulation endpoint called: {}", errorType);

        try {
            ErrorType type = ErrorType.valueOf(errorType.toUpperCase());
            errorSimulationService.simulateSpecificError(type);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "This should not be reached");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid error type: " + errorType);
            error.put("availableTypes", ErrorType.values());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 설정 조회 API
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("periodicErrorEnabled", config.isPeriodicErrorEnabled());
        configMap.put("periodicErrorIntervalSeconds", config.getPeriodicErrorIntervalSeconds());
        configMap.put("periodicErrorRate", config.getPeriodicErrorRate());
        configMap.put("apiErrorEnabled", config.isApiErrorEnabled());
        configMap.put("apiErrorRate", config.getApiErrorRate());

        Map<String, Integer> weights = new HashMap<>();
        weights.put("internalError", config.getInternalErrorWeight());
        weights.put("badRequest", config.getBadRequestWeight());
        weights.put("notFound", config.getNotFoundWeight());
        weights.put("serviceUnavailable", config.getServiceUnavailableWeight());
        weights.put("timeout", config.getTimeoutWeight());
        weights.put("databaseError", config.getDatabaseErrorWeight());
        weights.put("externalApiError", config.getExternalApiErrorWeight());
        weights.put("memoryError", config.getMemoryErrorWeight());
        configMap.put("errorTypeWeights", weights);

        return ResponseEntity.ok(configMap);
    }

    /**
     * 주기적 에러 활성화/비활성화 API
     */
    @PostMapping("/periodic/toggle")
    public ResponseEntity<Map<String, Object>> togglePeriodicError(@RequestParam boolean enabled) {
        config.setPeriodicErrorEnabled(enabled);
        logger.info("Periodic error simulation {}", enabled ? "enabled" : "disabled");

        Map<String, Object> response = new HashMap<>();
        response.put("periodicErrorEnabled", enabled);
        response.put("message", "Periodic error simulation " + (enabled ? "enabled" : "disabled"));
        return ResponseEntity.ok(response);
    }

    /**
     * API 에러 활성화/비활성화 API
     */
    @PostMapping("/api/toggle")
    public ResponseEntity<Map<String, Object>> toggleApiError(@RequestParam boolean enabled) {
        config.setApiErrorEnabled(enabled);
        logger.info("API error simulation {}", enabled ? "enabled" : "disabled");

        Map<String, Object> response = new HashMap<>();
        response.put("apiErrorEnabled", enabled);
        response.put("message", "API error simulation " + (enabled ? "enabled" : "disabled"));
        return ResponseEntity.ok(response);
    }

    /**
     * 에러율 설정 API
     */
    @PostMapping("/rate")
    public ResponseEntity<Map<String, Object>> setErrorRate(
            @RequestParam(required = false) Integer periodicRate,
            @RequestParam(required = false) Integer apiRate) {

        if (periodicRate != null) {
            config.setPeriodicErrorRate(periodicRate);
        }
        if (apiRate != null) {
            config.setApiErrorRate(apiRate);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("periodicErrorRate", config.getPeriodicErrorRate());
        response.put("apiErrorRate", config.getApiErrorRate());
        response.put("message", "Error rates updated");
        return ResponseEntity.ok(response);
    }

    /**
     * 통계 조회 API
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = errorSimulationService.getStatistics();
        stats.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(stats);
    }

    /**
     * 통계 초기화 API
     */
    @PostMapping("/statistics/reset")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        errorSimulationService.resetStatistics();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Statistics reset successfully");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * 사용 가능한 에러 타입 목록 API
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getErrorTypes() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> types = new HashMap<>();

        for (ErrorType errorType : ErrorType.values()) {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("httpStatus", errorType.getStatus().value());
            typeInfo.put("statusText", errorType.getStatus().getReasonPhrase());
            typeInfo.put("sampleMessages", errorType.getAllMessages());
            types.put(errorType.name(), typeInfo);
        }

        response.put("errorTypes", types);
        return ResponseEntity.ok(response);
    }

    /**
     * 예외 핸들러
     */
    @ExceptionHandler(SimulatedErrorException.class)
    public ResponseEntity<Map<String, Object>> handleSimulatedError(SimulatedErrorException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getErrorType().name());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("httpStatus", e.getErrorType().getStatus().value());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("simulated", true);

        logger.error("Simulated error response: {}", errorResponse);

        return ResponseEntity
                .status(e.getErrorType().getStatus())
                .body(errorResponse);
    }

    /**
     * 일반 예외 핸들러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception e) {
        logger.error("Unexpected error in error simulation controller", e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "INTERNAL_SERVER_ERROR");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
