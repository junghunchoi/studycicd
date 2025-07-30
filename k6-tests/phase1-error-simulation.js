import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 에러 시뮬레이션을 위한 메트릭
const errorsByVersion = new Counter('errors_by_version');
const requestsByEndpoint = new Counter('requests_by_endpoint');

export const options = {
  stages: [
    { duration: '1m', target: 5 },   // 천천히 시작
    { duration: '3m', target: 15 },  // 부하 증가
    { duration: '1m', target: 0 },   // 종료
  ],
};

export default function () {
  const baseUrl = 'http://localhost:8000';
  
  // 다양한 엔드포인트 테스트 (에러 발생 시뮬레이션)
  const endpoints = [
    '/api/test',
    '/api/health',
    '/api/metrics',
    '/health',
    '/nonexistent', // 404 에러 발생
  ];
  
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const response = http.get(`${baseUrl}${endpoint}`, {
    headers: {
      'User-Agent': 'K6-ErrorSimulation/1.0',
      'X-Test-Scenario': 'error-simulation',
    },
  });

  requestsByEndpoint.add(1, { endpoint: endpoint });

  const success = check(response, {
    'status is not 5xx': (r) => r.status < 500,
    'response received': (r) => r.body.length > 0,
  });

  // 에러 발생 시 상세 로깅
  if (!success || response.status >= 400) {
    const upstreamServer = response.headers['X-Upstream-Server'];
    const version = upstreamServer && upstreamServer.includes('legacy') ? 'legacy' : 'canary';
    
    errorsByVersion.add(1, { 
      version: version,
      status: response.status,
      endpoint: endpoint 
    });
    
    console.log(`ERROR: ${endpoint} -> ${response.status} (${version})`);
  }

  // Request ID 추적을 위한 로깅
  const requestId = response.headers['X-Request-ID'];
  if (requestId) {
    console.log(`RequestID: ${requestId}, Status: ${response.status}, Endpoint: ${endpoint}`);
  }

  sleep(0.2);
}

export function handleSummary(data) {
  console.log('\n=== Phase 1.2: Error Simulation Results ===');
  console.log('Use these LogQL queries in Grafana to analyze:');
  console.log('1. Error rate by version:');
  console.log('   (count_over_time({job="spring-boot"} |= "ERROR"[5m]) / count_over_time({job="spring-boot"}[5m])) * 100');
  console.log('2. 4xx/5xx errors:');
  console.log('   {job="nginx"} |~ "status=[45][0-9][0-9]"');
  console.log('3. Trace specific request:');
  console.log('   {job=~"nginx|spring-boot"} |~ "RequestID_HERE"');
  
  return {
    'error-simulation-summary.json': JSON.stringify(data, null, 2),
  };
}