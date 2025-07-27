import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('custom_error_rate');
const requestCounter = new Counter('custom_requests_total');
const responseTime = new Trend('custom_response_time');

// 부하 테스트 설정
export const options = {
  scenarios: {
    // 점진적 부하 증가
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 20 },  // 2분 동안 20 VU까지 증가
        { duration: '5m', target: 20 },  // 5분 동안 20 VU 유지
        { duration: '2m', target: 50 },  // 2분 동안 50 VU까지 증가
        { duration: '5m', target: 50 },  // 5분 동안 50 VU 유지
        { duration: '2m', target: 0 },   // 2분 동안 0까지 감소
      ],
      exec: 'loadTest',
    },
    // 스파이크 테스트
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 }, // 급격한 증가
        { duration: '1m', target: 100 },  // 1분 유지
        { duration: '30s', target: 0 },   // 급격한 감소
      ],
      exec: 'spikeTest',
      startTime: '10m', // 로드 테스트 후 실행
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 미만
    http_req_failed: ['rate<0.05'],    // 에러율 5% 미만
    custom_error_rate: ['rate<0.05'],
    custom_response_time: ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const CONTROLLER_URL = __ENV.CONTROLLER_URL || 'http://localhost:8080';

// 부하 테스트
export function loadTest() {
  const endpoints = [
    { path: '/api/hello', method: 'GET', weight: 50 },
    { path: '/api/test', method: 'GET', weight: 30 },
    { path: '/api/version', method: 'GET', weight: 15 },
    { path: '/api/error-simulation', method: 'GET', weight: 5 },
  ];
  
  // 가중치 기반 엔드포인트 선택
  const random = Math.random() * 100;
  let cumulative = 0;
  let selectedEndpoint = endpoints[0];
  
  for (const endpoint of endpoints) {
    cumulative += endpoint.weight;
    if (random <= cumulative) {
      selectedEndpoint = endpoint;
      break;
    }
  }
  
  const startTime = Date.now();
  const response = http.get(`${BASE_URL}${selectedEndpoint.path}`, {
    tags: { 
      endpoint: selectedEndpoint.path,
      test_type: 'load_test'
    },
  });
  const duration = Date.now() - startTime;
  
  // 커스텀 메트릭 기록
  requestCounter.add(1);
  responseTime.add(duration);
  errorRate.add(response.status !== 200);
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 2s': (r) => r.timings.duration < 2000,
    'has version info': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.version !== undefined;
      } catch {
        return false;
      }
    },
  });
  
  if (!success) {
    console.log(`Request failed: ${selectedEndpoint.path} - Status: ${response.status}`);
  }
  
  // 사용자 행동 시뮬레이션
  sleep(0.1 + Math.random() * 0.5); // 100ms ~ 600ms
}

// 스파이크 테스트
export function spikeTest() {
  const response = http.get(`${BASE_URL}/api/hello`, {
    tags: { test_type: 'spike_test' },
  });
  
  check(response, {
    'spike test - status is 200': (r) => r.status === 200,
    'spike test - response time < 5s': (r) => r.timings.duration < 5000,
  });
  
  // 스파이크 테스트에서는 더 짧은 간격
  sleep(0.05 + Math.random() * 0.1); // 50ms ~ 150ms
}

// 테스트 시작 시 실행
export function setup() {
  console.log('=== 부하 테스트 시작 ===');
  
  // 초기 상태 확인
  const statusResponse = http.get(`${CONTROLLER_URL}/api/traffic/status`);
  if (statusResponse.status === 200) {
    const status = JSON.parse(statusResponse.body);
    console.log(`Initial traffic status: Legacy ${status.legacyWeight}%, Refactored ${status.refactoredWeight}%`);
  }
  
  // 테스트 환경 설정
  const adjustResponse = http.post(`${CONTROLLER_URL}/api/traffic/adjust`, 
    JSON.stringify({ legacyWeight: 50, refactoredWeight: 50 }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (adjustResponse.status === 200) {
    console.log('Traffic set to 50/50 for load testing');
  }
  
  return { startTime: Date.now() };
}

// 테스트 종료 시 실행
export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  console.log(`=== 부하 테스트 완료 (${duration}초) ===`);
  
  // 최종 메트릭 확인
  const metricsResponse = http.get(`${CONTROLLER_URL}/api/metrics/current`);
  if (metricsResponse.status === 200) {
    const metrics = JSON.parse(metricsResponse.body);
    console.log(`Final metrics - Error Rate: ${metrics.errorRate}%, Response Time: ${metrics.averageResponseTime}s`);
  }
  
  // 트래픽을 초기 상태로 복원
  const resetResponse = http.post(`${CONTROLLER_URL}/api/traffic/adjust`, 
    JSON.stringify({ legacyWeight: 95, refactoredWeight: 5 }), {
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (resetResponse.status === 200) {
    console.log('Traffic reset to initial state (95/5)');
  }
}