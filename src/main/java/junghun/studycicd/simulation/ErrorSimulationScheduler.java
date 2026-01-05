package junghun.studycicd.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ErrorSimulationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorSimulationScheduler.class);
    private final ErrorSimulationService errorSimulationService;
    private final ErrorSimulationConfig config;

    public ErrorSimulationScheduler(ErrorSimulationService errorSimulationService,
                                     ErrorSimulationConfig config) {
        this.errorSimulationService = errorSimulationService;
        this.config = config;
    }

    /**
     * 주기적으로 에러를 시뮬레이션
     * 설정된 간격마다 실행되며, 설정에 따라 에러를 발생시킴
     */
    @Scheduled(fixedRateString = "${error-simulation.periodic.interval:30000}")
    public void simulatePeriodicErrors() {
        if (!config.isPeriodicErrorEnabled()) {
            return;
        }

        try {
            errorSimulationService.simulatePeriodicError();
        } catch (Exception e) {
            logger.error("Unexpected error during periodic error simulation", e);
        }
    }

    /**
     * 통계 정보를 주기적으로 로깅
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void logStatistics() {
        if (!config.isPeriodicErrorEnabled() && !config.isApiErrorEnabled()) {
            return;
        }

        var stats = errorSimulationService.getStatistics();
        logger.info("Error Simulation Statistics: {}", stats);
    }
}
