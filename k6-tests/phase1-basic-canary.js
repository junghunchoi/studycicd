import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
const legacyRequests = new Counter('legacy_requests');
const canaryRequests = new Counter('canary_requests');
const errorRate = new Rate('error_rate');
const requestDuration = new Trend('request_duration');

export const options = {
  stages: [
    { duration: '1m', target: 10 },  // 워밍업
    { duration: '3m', target: 20 },  // 정상 부하
    // { duration: '2m', target: 0 },   // 쿨다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% 요청이 500ms 이내
    error_rate: ['rate<0.05'],         // 에러율 5% 미만
  },
};

export default function () {
  const baseUrl = 'http://localhost:8000';
  
  // Phase 1.1: 기본 카나리 배포 테스트
  const response = http.get(`${baseUrl}/api/test`, {
    headers: {
      'User-Agent': 'K6-CanaryTest/1.0',
    },
  });

  // 응답 검증
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has upstream header': (r) => r.headers['X-Upstream-Server'] !== undefined,
  });

  // 에러율 추적
  errorRate.add(!success);
  requestDuration.add(response.timings.duration);

  // 업스트림 서버 추적
  const upstreamServer = response.headers['X-Upstream-Server'];
  if (upstreamServer && upstreamServer.includes('legacy')) {
    legacyRequests.add(1);
  } else if (upstreamServer && upstreamServer.includes('refactored')) {
    canaryRequests.add(1);
  }

  // Request ID 로깅 (Grafana에서 추적 가능)
  console.log(`Request ID: ${response.headers['X-Request-ID'] || 'unknown'}, Upstream: ${upstreamServer}`);

  sleep(0.1);
}

export function handleSummary(data) {
  const totalRequests = data.metrics.http_reqs?.values?.count || 0;
  const legacyCount = data.metrics.legacy_requests?.values?.count || 0;
  const canaryCount = data.metrics.canary_requests?.values?.count || 0;

  console.log('\n=== Phase 1: Basic Canary Deployment Results ===');
  console.log(`Total Requests: ${totalRequests}`);
  console.log(`Legacy Requests: ${legacyCount} (${totalRequests > 0 ? ((legacyCount/totalRequests)*100).toFixed(1) : 0}%)`);
  console.log(`Canary Requests: ${canaryCount} (${totalRequests > 0 ? ((canaryCount/totalRequests)*100).toFixed(1) : 0}%)`);
  console.log(`Error Rate: ${(data.metrics.error_rate?.values?.rate * 100).toFixed(2)}%`);
  console.log(`P95 Response Time: ${data.metrics.request_duration?.values?.['p(95)'].toFixed(2)}ms`);

  return {
    'summary.json': JSON.stringify(data, null, 2),
  };
}