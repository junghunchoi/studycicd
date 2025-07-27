import http from 'k6/http';
import { check, sleep } from 'k6';

// 카나리 배포 시뮬레이션 테스트
export const options = {
  scenarios: {
    // 카나리 배포 시뮬레이션
    canary_simulation: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: 'simulateCanaryDeployment',
    },
    // 지속적인 트래픽 생성
    traffic_generator: {
      executor: 'constant-vus',
      vus: 20,
      duration: '10m',
      exec: 'generateTraffic',
      startTime: '5s',
    },
    // 메트릭 모니터링
    metrics_monitor: {
      executor: 'constant-vus',
      vus: 1,
      duration: '10m',
      exec: 'monitorMetrics',
      startTime: '10s',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const CONTROLLER_URL = __ENV.CONTROLLER_URL || 'http://localhost:8080';

// 카나리 배포 시뮬레이션
export function simulateCanaryDeployment() {
  console.log('=== 카나리 배포 시뮬레이션 시작 ===');
  
  // 초기 상태 확인
  let response = http.get(`${CONTROLLER_URL}/api/traffic/canary/status`);
  console.log(`초기 상태: ${response.body}`);
  
  // 1. 카나리 배포 시작
  console.log('1. 카나리 배포 시작');
  response = http.post(`${CONTROLLER_URL}/api/traffic/canary/start`);
  check(response, {
    'deployment started': (r) => r.status === 200,
  });
  console.log(`배포 시작: ${response.body}`);
  
  // 2분 대기 (5% 트래픽으로 안정성 확인)
  console.log('2. Stage 1 (5%) - 2분 대기 중...');
  sleep(120);
  
  // 2. 다음 단계로 진행 (10%)
  console.log('2. Stage 2 (10%)로 진행');
  response = http.post(`${CONTROLLER_URL}/api/traffic/canary/next-stage`);
  check(response, {
    'stage 2 success': (r) => r.status === 200,
  });
  console.log(`Stage 2: ${response.body}`);
  
  // 2분 대기
  console.log('Stage 2 (10%) - 2분 대기 중...');
  sleep(120);
  
  // 3. 다음 단계로 진행 (25%)
  console.log('3. Stage 3 (25%)로 진행');
  response = http.post(`${CONTROLLER_URL}/api/traffic/canary/next-stage`);
  check(response, {
    'stage 3 success': (r) => r.status === 200,
  });
  console.log(`Stage 3: ${response.body}`);
  
  // 2분 대기
  console.log('Stage 3 (25%) - 2분 대기 중...');
  sleep(120);
  
  // 메트릭 확인 후 결정
  response = http.get(`${CONTROLLER_URL}/api/metrics/health`);
  const metricsData = JSON.parse(response.body);
  
  if (metricsData.isHealthy) {
    // 4. 다음 단계로 진행 (50%)
    console.log('4. Stage 4 (50%)로 진행');
    response = http.post(`${CONTROLLER_URL}/api/traffic/canary/next-stage`);
    console.log(`Stage 4: ${response.body}`);
    
    sleep(120);
    
    // 5. 최종 단계로 진행 (100%)
    console.log('5. 최종 Stage (100%)로 진행');
    response = http.post(`${CONTROLLER_URL}/api/traffic/canary/next-stage`);
    console.log(`Final Stage: ${response.body}`);
  } else {
    // 롤백 실행
    console.log('메트릭이 좋지 않음. 롤백 실행.');
    response = http.post(`${CONTROLLER_URL}/api/traffic/canary/rollback`);
    console.log(`Rollback: ${response.body}`);
  }
  
  console.log('=== 카나리 배포 시뮬레이션 완료 ===');
}

// 지속적인 트래픽 생성
export function generateTraffic() {
  const endpoints = [
    { path: '/api/hello', weight: 40 },
    { path: '/api/test', weight: 30 },
    { path: '/api/version', weight: 20 },
    { path: '/api/error-simulation', weight: 10 },
  ];
  
  // 가중치 기반 엔드포인트 선택
  const random = Math.random() * 100;
  let cumulative = 0;
  let selectedEndpoint = endpoints[0].path;
  
  for (const endpoint of endpoints) {
    cumulative += endpoint.weight;
    if (random <= cumulative) {
      selectedEndpoint = endpoint.path;
      break;
    }
  }
  
  const response = http.get(`${BASE_URL}${selectedEndpoint}`, {
    tags: { 
      endpoint: selectedEndpoint,
      test_type: 'canary_traffic'
    },
  });
  
  check(response, {
    'request successful': (r) => r.status >= 200 && r.status < 300,
    'response time < 2s': (r) => r.timings.duration < 2000,
  });
  
  // 실제 사용자 패턴 시뮬레이션
  sleep(0.1 + Math.random() * 0.9); // 100ms ~ 1000ms 간격
}

// 메트릭 모니터링
export function monitorMetrics() {
  const response = http.get(`${CONTROLLER_URL}/api/metrics/current`);
  
  if (response.status === 200) {
    const metrics = JSON.parse(response.body);
    
    check(response, {
      'metrics available': (r) => r.status === 200,
      'error rate acceptable': (r) => {
        const data = JSON.parse(r.body);
        return data.errorRate <= 5.0; // 5% 이하
      },
      'response time acceptable': (r) => {
        const data = JSON.parse(r.body);
        return data.averageResponseTime <= 2.0; // 2초 이하
      },
    });
    
    // 메트릭 로깅 (매 30초마다)
    if (__ITER % 30 === 0) {
      console.log(`[Metrics] Error Rate: ${metrics.errorRate}%, ` +
                 `Avg Response Time: ${metrics.averageResponseTime}s, ` +
                 `Healthy: ${metrics.isHealthy}`);
    }
  }
  
  sleep(1); // 1초마다 체크
}