package junghun.studycicd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 자동화된 카나리 배포 스케줄러
 * 실무에서 사용되는 점진적 트래픽 전환 및 SLI/SLO 기반 자동 의사결정 시스템
 */
@Service
public class AutoDeploymentScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(AutoDeploymentScheduler.class);
    
    private final DeploymentService deploymentService;
    private final SliSloEvaluator sliSloEvaluator;
    
    // 자동 배포 설정
    @Value("${auto-deployment.enabled:true}")
    private Boolean autoDeploymentEnabled;
    
    @Value("${auto-deployment.stage-wait-minutes:5}")
    private Integer stageWaitMinutes;
    
    @Value("${auto-deployment.evaluation-period-minutes:3}")
    private Integer evaluationPeriodMinutes;
    
    @Value("${auto-deployment.min-sample-size:100}")
    private Integer minSampleSize;
    
    // 자동 배포 상태
    private final AtomicBoolean autoDeploymentInProgress = new AtomicBoolean(false);
    private LocalDateTime lastStageTransition;
    private LocalDateTime stageStartTime;
    private String currentAutoDeploymentId;
    
    public AutoDeploymentScheduler(DeploymentService deploymentService, 
                                  SliSloEvaluator sliSloEvaluator) {
        this.deploymentService = deploymentService;
        this.sliSloEvaluator = sliSloEvaluator;
    }
    
    /**
     * 자동 카나리 배포 시작
     */
    public boolean startAutoDeployment() {
        if (!autoDeploymentEnabled) {
            logger.warn("Auto deployment is disabled");
            return false;
        }
        
        if (autoDeploymentInProgress.get()) {
            logger.warn("Auto deployment already in progress");
            return false;
        }
        
        var deploymentStatus = deploymentService.startCanaryDeployment();
        if ("DEPLOYING".equals(deploymentStatus.getStatus())) {
            autoDeploymentInProgress.set(true);
            currentAutoDeploymentId = deploymentStatus.getDeploymentId();
            stageStartTime = LocalDateTime.now();
            lastStageTransition = LocalDateTime.now();
            
            logger.info("🚀 Auto deployment started: {} - Stage {}/{}",
                       currentAutoDeploymentId, 
                       deploymentStatus.getCurrentStage() + 1,
                       deploymentStatus.getTotalStages());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 자동 배포 중단
     */
    public void stopAutoDeployment() {
        if (autoDeploymentInProgress.get()) {
            logger.info("⏹️ Auto deployment stopped manually: {}", currentAutoDeploymentId);
            autoDeploymentInProgress.set(false);
            currentAutoDeploymentId = null;
            stageStartTime = null;
            lastStageTransition = null;
        }
    }
    
    /**
     * 자동 배포 프로세스 실행 (매 1분마다 실행)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void processAutoDeployment() {
        if (!autoDeploymentEnabled || !autoDeploymentInProgress.get()) {
            return;
        }
        
        try {
            var deploymentStatus = deploymentService.getDeploymentStatus();
            
            // 배포가 완료되었거나 실패한 경우
            if (!"DEPLOYING".equals(deploymentStatus.getStatus())) {
                autoDeploymentInProgress.set(false);
                
                if ("STABLE".equals(deploymentStatus.getStatus()) && 
                    deploymentStatus.getCurrentStage() >= deploymentStatus.getTotalStages()) {
                    logger.info("✅ Auto deployment completed successfully: {}", currentAutoDeploymentId);
                } else {
                    logger.warn("❌ Auto deployment terminated: {} - Status: {}", 
                               currentAutoDeploymentId, deploymentStatus.getStatus());
                }
                
                currentAutoDeploymentId = null;
                stageStartTime = null;
                lastStageTransition = null;
                return;
            }
            
            // 최소 대기 시간 확인
            if (stageStartTime != null) {
                long minutesSinceStageStart = ChronoUnit.MINUTES.between(stageStartTime, LocalDateTime.now());
                if (minutesSinceStageStart < stageWaitMinutes) {
                    logger.debug("⏳ Waiting for minimum stage duration: {}/{} minutes", 
                               minutesSinceStageStart, stageWaitMinutes);
                    return;
                }
            }
            
            // SLI/SLO 평가
            var sliSloResult = sliSloEvaluator.evaluateCurrentState();
            
            // 충분한 샘플 수 확인
            if (sliSloResult.getSampleSize() < minSampleSize) {
                logger.debug("📊 Insufficient sample size: {}/{}", 
                           sliSloResult.getSampleSize(), minSampleSize);
                return;
            }
            
            // SLO 위반 시 자동 롤백
            if (!sliSloResult.isSloCompliant()) {
                logger.warn("⚠️ SLO violation detected - triggering automatic rollback");
                logger.warn("SLO Details: Error Rate: {:.2f}% (Max: {:.2f}%), " +
                           "Response Time P95: {:.2f}s (Max: {:.2f}s)",
                           sliSloResult.getErrorRatePercent(),
                           sliSloResult.getErrorRateThreshold(),
                           sliSloResult.getResponseTimeP95(),
                           sliSloResult.getResponseTimeThreshold());
                
                deploymentService.rollbackDeployment();
                autoDeploymentInProgress.set(false);
                currentAutoDeploymentId = null;
                stageStartTime = null;
                lastStageTransition = null;
                return;
            }
            
            // SLO가 만족되면 다음 단계로 진행
            logger.info("📈 SLO compliance verified - proceeding to next stage");
            logger.info("Current Metrics: Error Rate: {:.2f}%, Response Time P95: {:.2f}s, Samples: {}",
                       sliSloResult.getErrorRatePercent(),
                       sliSloResult.getResponseTimeP95(),
                       sliSloResult.getSampleSize());
            
            var nextStageResult = deploymentService.proceedToNextStage();
            
            if ("DEPLOYING".equals(nextStageResult.getStatus())) {
                stageStartTime = LocalDateTime.now();
                lastStageTransition = LocalDateTime.now();
                
                logger.info("🎯 Advanced to stage {}/{} - {}% traffic to refactored version",
                           nextStageResult.getCurrentStage() + 1,
                           nextStageResult.getTotalStages(),
                           nextStageResult.getCurrentPercentage());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error in auto deployment process", e);
            
            // 에러 발생 시 안전을 위해 롤백
            logger.warn("⚠️ Error detected - triggering safety rollback");
            deploymentService.rollbackDeployment();
            autoDeploymentInProgress.set(false);
            currentAutoDeploymentId = null;
            stageStartTime = null;
            lastStageTransition = null;
        }
    }
    
    /**
     * 자동 배포 상태 정보
     */
    public AutoDeploymentStatus getAutoDeploymentStatus() {
        var deploymentStatus = deploymentService.getDeploymentStatus();
        
        return new AutoDeploymentStatus(
            autoDeploymentEnabled,
            autoDeploymentInProgress.get(),
            currentAutoDeploymentId,
            deploymentStatus,
            stageStartTime,
            lastStageTransition,
            stageWaitMinutes,
            evaluationPeriodMinutes,
            minSampleSize
        );
    }
    
    /**
     * 자동 배포 상태 정보 클래스
     */
    public static class AutoDeploymentStatus {
        private final Boolean enabled;
        private final Boolean inProgress;
        private final String deploymentId;
        private final Object deploymentStatus;
        private final LocalDateTime stageStartTime;
        private final LocalDateTime lastTransition;
        private final Integer stageWaitMinutes;
        private final Integer evaluationPeriodMinutes;
        private final Integer minSampleSize;
        
        public AutoDeploymentStatus(Boolean enabled, Boolean inProgress, String deploymentId,
                                  Object deploymentStatus, LocalDateTime stageStartTime,
                                  LocalDateTime lastTransition, Integer stageWaitMinutes,
                                  Integer evaluationPeriodMinutes, Integer minSampleSize) {
            this.enabled = enabled;
            this.inProgress = inProgress;
            this.deploymentId = deploymentId;
            this.deploymentStatus = deploymentStatus;
            this.stageStartTime = stageStartTime;
            this.lastTransition = lastTransition;
            this.stageWaitMinutes = stageWaitMinutes;
            this.evaluationPeriodMinutes = evaluationPeriodMinutes;
            this.minSampleSize = minSampleSize;
        }
        
        // Getters
        public Boolean getEnabled() { return enabled; }
        public Boolean getInProgress() { return inProgress; }
        public String getDeploymentId() { return deploymentId; }
        public Object getDeploymentStatus() { return deploymentStatus; }
        public LocalDateTime getStageStartTime() { return stageStartTime; }
        public LocalDateTime getLastTransition() { return lastTransition; }
        public Integer getStageWaitMinutes() { return stageWaitMinutes; }
        public Integer getEvaluationPeriodMinutes() { return evaluationPeriodMinutes; }
        public Integer getMinSampleSize() { return minSampleSize; }
    }
}