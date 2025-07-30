import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// 프로덕션 규모 메트릭
const realTimeUsers = new Gauge('real_time_users');
const businessTransactions = new Counter('business_transactions');
const systemLoad = new Trend('system_load');
const dataConsistency = new Rate('data_consistency');

export const options = {
  scenarios: {
    // 대용량 트래픽 시뮬레이션
    production_load: {
      executor: 'ramping-vus',
      stages: [
        { duration: '5m', target: 50 },   // 출근 시간
        { duration: '10m', target: 100 }, // 피크 타임
        { duration: '5m', target: 200 },  // 점심 시간 급증
        { duration: '10m', target: 150 }, // 오후 안정
        { duration: '5m', target: 300 },  // 저녁 피크
        { duration: '10m', target: 100 }, // 야간 감소
        { duration: '5m', target: 0 },    // 심야
      ],
    },
    // 지속적인 헬스체크
    health_monitoring: {
      executor: 'constant-vus',
      vus: 2,
      duration: '50m',
      exec: 'healthCheck',
    },
    // 부하 테스트
    stress_test: {
      executor: 'constant-arrival-rate',
      rate: 100, // 초당 100 요청
      timeUnit: '1s',
      duration: '20m',
      preAllocatedVUs: 50,
      maxVUs: 200,
      exec: 'stressTest',
    },
  },
  thresholds: {
    // 프로덕션 SLO
    http_req_duration: [
      'p(50)<200',   // 50% 요청이 200ms 이내
      'p(90)<500',   // 90% 요청이 500ms 이내
      'p(95)<1000',  // 95% 요청이 1초 이내
      'p(99)<2000',  // 99% 요청이 2초 이내
    ],
    http_req_failed: ['rate<0.01'], // 1% 미만 실패율
    business_transactions: ['count>1000'], // 최소 1000건 비즈니스 트랜잭션
  },
};

// 실제 사용자 행동 패턴
const userBehaviors = [
  {
    type: 'browser',
    weight: 60,
    actions: [
      { endpoint: '/health', probability: 0.1 },
      { endpoint: '/api/test', probability: 0.4 },
      { endpoint: '/api/metrics', probability: 0.3 },
      { endpoint: '/api/heavy-operation', probability: 0.2 },
    ]
  },
  {
    type: 'mobile_app',
    weight: 30,
    actions: [
      { endpoint: '/api/test', probability: 0.6 },
      { endpoint: '/api/quick-data', probability: 0.4 },
    ]
  },
  {
    type: 'api_client',
    weight: 10,
    actions: [
      { endpoint: '/api/metrics', probability: 0.5 },
      { endpoint: '/api/bulk-operation', probability: 0.5 },
    ]
  },
];

export default function () {
  const baseUrl = 'http://localhost:8000';
  const behavior = selectBehavior();
  const action = selectAction(behavior);
  
  const sessionId = `session_${__VU}_${Math.floor(__ITER / 10)}`;
  
  realTimeUsers.add(__VU);
  
  const response = http.get(`${baseUrl}${action.endpoint}`, {
    headers: {
      'User-Agent': `${behavior.type}/1.0`,
      'X-Session-ID': sessionId,
      'X-User-Behavior': behavior.type,
      'X-Test-Scenario': 'production-load',
    },
  });
  
  const upstreamServer = response.headers['X-Upstream-Server'];
  const version = upstreamServer && upstreamServer.includes('legacy') ? 'legacy' : 'canary';
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time acceptable': (r) => r.timings.duration < 2000,
    'content is valid': (r) => r.body && r.body.length > 0,
  });
  
  // 비즈니스 트랜잭션 추적
  if (success && action.endpoint.includes('api')) {
    businessTransactions.add(1, { 
      version: version, 
      behavior: behavior.type,
      endpoint: action.endpoint 
    });
  }
  
  // 시스템 부하 시뮬레이션
  systemLoad.add(response.timings.duration, { version: version });
  
  // 데이터 일관성 체크 (간단한 시뮬레이션)
  if (action.endpoint.includes('data')) {
    const isConsistent = response.headers['X-Data-Version'] === response.headers['X-Expected-Version'];
    dataConsistency.add(isConsistent, { version: version });
  }
  
  // 사용자 행동에 따른 대기 시간
  const thinkTime = behavior.type === 'api_client' ? 0.05 : 
                   behavior.type === 'mobile_app' ? 0.3 : 0.8;
  sleep(Math.random() * thinkTime);
}

// 헬스체크 시나리오
export function healthCheck() {
  const baseUrl = 'http://localhost:8000';
  
  const healthResponse = http.get(`${baseUrl}/health`, {
    headers: { 'X-Monitor': 'health-check' }
  });
  
  check(healthResponse, {
    'health check passes': (r) => r.status === 200,
  });
  
  sleep(30); // 30초마다 헬스체크
}

// 스트레스 테스트 시나리오
export function stressTest() {
  const baseUrl = 'http://localhost:8000';
  
  // 다양한 부하 패턴
  const endpoints = [
    '/api/test',
    '/api/metrics', 
    '/api/heavy-operation',
    '/health'
  ];
  
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  
  const response = http.get(`${baseUrl}${endpoint}`, {
    headers: {
      'X-Test-Type': 'stress-test',
      'X-Load-Level': 'high',
    }
  });
  
  check(response, {
    'survives stress': (r) => r.status < 500,
    'responds under load': (r) => r.timings.duration < 5000,
  });
}

function selectBehavior() {
  const random = Math.random() * 100;
  let cumulative = 0;
  
  for (const behavior of userBehaviors) {
    cumulative += behavior.weight;
    if (random <= cumulative) {
      return behavior;
    }
  }
  return userBehaviors[0];
}

function selectAction(behavior) {
  const random = Math.random();
  let cumulative = 0;
  
  for (const action of behavior.actions) {
    cumulative += action.probability;
    if (random <= cumulative) {
      return action;
    }
  }
  return behavior.actions[0];
}

export function handleSummary(data) {
  const totalTransactions = data.metrics.business_transactions?.values?.count || 0;
  const p95ResponseTime = data.metrics.http_req_duration?.values?.['p(95)'] || 0;
  const errorRate = data.metrics.http_req_failed?.values?.rate || 0;
  
  console.log('\n=== Phase 5: Production Load Test Results ===');
  console.log(`Total Business Transactions: ${totalTransactions}`);
  console.log(`P95 Response Time: ${p95ResponseTime.toFixed(2)}ms`);
  console.log(`Error Rate: ${(errorRate * 100).toFixed(3)}%`);
  console.log(`Peak Concurrent Users: ${data.metrics.real_time_users?.values?.max || 0}`);
  
  console.log('\nProduction Monitoring Queries:');
  console.log('1. Real-time performance:');
  console.log('   histogram_quantile(0.95, sum(rate({job="nginx"}[1m])) by (le))');
  console.log('2. Business transaction rate:');
  console.log('   rate({job="nginx"} |~ "api"[5m])');
  console.log('3. System health:');
  console.log('   up{job="nginx"} and up{job="spring-boot"}');
  console.log('4. Canary vs Legacy performance:');
  console.log('   avg by (version) (rate({job="nginx"}[5m]))');
  
  return {
    'production-load-summary.json': JSON.stringify(data, null, 2),
  };
}