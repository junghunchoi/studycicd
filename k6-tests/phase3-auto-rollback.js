import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 자동 롤백 시뮬레이션 메트릭
const criticalErrors = new Counter('critical_errors');
const rollbackTriggers = new Counter('rollback_triggers');
const recoveryTime = new Trend('recovery_time');

export const options = {
  scenarios: {
    // 장애 상황 시뮬레이션
    failure_simulation: {
      executor: 'ramping-vus',
      stages: [
        { duration: '2m', target: 20 },  // 정상 상태
        { duration: '1m', target: 40 },  // 부하 증가 (장애 유발)
        { duration: '3m', target: 40 },  // 장애 지속
        { duration: '2m', target: 20 },  // 복구 후 안정화
      ],
    },
  },
  thresholds: {
    // SLO 정의 - 이 임계값을 넘으면 롤백 트리거
    http_req_failed: ['rate<0.05'],      // 5% 미만 실패율
    http_req_duration: ['p(95)<1000'],   // 95% 요청이 1초 이내
    critical_errors: ['count<10'],        // 심각한 에러 10개 미만
  },
};

let rollbackDetected = false;
let rollbackStartTime = null;

export default function () {
  const baseUrl = 'http://localhost:8000';
  
  // 장애 시뮬레이션을 위한 다양한 요청 패턴
  const testScenarios = [
    { endpoint: '/api/test', errorProbability: 0.02 },      // 정상적인 엔드포인트
    { endpoint: '/api/health', errorProbability: 0.01 },    // 헬스체크
    { endpoint: '/api/heavy-load', errorProbability: 0.15 }, // 무거운 작업 (에러 유발)
  ];
  
  const scenario = testScenarios[Math.floor(Math.random() * testScenarios.length)];
  
  // 의도적 장애 주입 (Phase 3에서 카나리 버전에 장애 발생)
  const shouldInjectFailure = Math.random() < scenario.errorProbability;
  const headers = {
    'User-Agent': 'K6-RollbackTest/1.0',
    'X-Test-Scenario': 'auto-rollback',
  };
  
  if (shouldInjectFailure) {
    headers['X-Inject-Error'] = 'true'; // 애플리케이션에서 이 헤더를 보고 에러 발생
  }

  const response = http.get(`${baseUrl}${scenario.endpoint}`, { headers });
  
  const upstreamServer = response.headers['X-Upstream-Server'];
  const version = upstreamServer && upstreamServer.includes('legacy') ? 'legacy' : 'canary';
  
  const success = check(response, {
    'status is not 5xx': (r) => r.status < 500,
    'response time < 2s': (r) => r.timings.duration < 2000,
    'no critical errors': (r) => r.status !== 503,
  });

  // 심각한 에러 감지
  if (response.status >= 500 || response.timings.duration > 2000) {
    criticalErrors.add(1, { 
      version: version, 
      error_type: response.status >= 500 ? 'server_error' : 'timeout',
      endpoint: scenario.endpoint 
    });
    
    console.log(`CRITICAL ERROR: ${scenario.endpoint} -> ${response.status} (${version}) - ${response.timings.duration.toFixed(0)}ms`);
    
    // 롤백 조건 체크 (실제로는 Prometheus Alert가 수행)
    if (!rollbackDetected && (response.status >= 500 || response.timings.duration > 2000)) {
      rollbackDetected = true;
      rollbackStartTime = Date.now();
      rollbackTriggers.add(1, { trigger_reason: response.status >= 500 ? 'error_rate' : 'latency' });
      
      console.log('🚨 ROLLBACK TRIGGERED! High error rate or latency detected');
      
      // 실제 환경에서는 여기서 롤백 스크립트 실행
      // curl -X POST http://controller:8080/rollback
    }
  }
  
  // 롤백 후 복구 시간 측정
  if (rollbackDetected && rollbackStartTime && success) {
    const timeSinceRollback = Date.now() - rollbackStartTime;
    if (timeSinceRollback > 30000) { // 30초 후 복구된 것으로 간주
      recoveryTime.add(timeSinceRollback);
      rollbackDetected = false; // 복구 완료
      console.log(`✅ RECOVERY COMPLETED: ${timeSinceRollback}ms`);
    }
  }

  sleep(0.1);
}

export function handleSummary(data) {
  const criticalErrorCount = data.metrics.critical_errors?.values?.count || 0;
  const rollbackCount = data.metrics.rollback_triggers?.values?.count || 0;
  const avgRecoveryTime = data.metrics.recovery_time?.values?.avg || 0;
  
  console.log('\n=== Phase 3.1: Auto Rollback Simulation Results ===');
  console.log(`Critical Errors Detected: ${criticalErrorCount}`);
  console.log(`Rollback Triggers: ${rollbackCount}`);
  console.log(`Average Recovery Time: ${avgRecoveryTime.toFixed(0)}ms`);
  
  console.log('\nMonitoring Queries for Auto-Rollback:');
  console.log('1. Error rate alert:');
  console.log('   (rate({job="nginx"} |~ "status=[45][0-9][0-9]"[2m]) / rate({job="nginx"}[2m])) > 0.05');
  console.log('2. Latency alert:');
  console.log('   histogram_quantile(0.95, rate({job="nginx"} | regex "request_time=(?P<time>[0-9.]+)"[2m])) > 1.0');
  console.log('3. Recovery tracking:');
  console.log('   {job="nginx"} |~ "RECOVERY COMPLETED"');
  
  return {
    'auto-rollback-summary.json': JSON.stringify(data, null, 2),
  };
}