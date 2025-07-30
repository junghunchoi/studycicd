import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// 트래픽 분산 추적 메트릭
const trafficDistribution = new Gauge('traffic_distribution');
const responseTimeByVersion = new Trend('response_time_by_version');
const throughputByVersion = new Counter('throughput_by_version');

export const options = {
  scenarios: {
    // 시나리오 2.1: 점진적 트래픽 증가 시뮬레이션
    gradual_increase: {
      executor: 'ramping-vus',
      stages: [
        { duration: '40s', target: 10 },  // 5% 카나리 (초기)
        { duration: '40s', target: 20 },  // 10% 카나리
        { duration: '40s', target: 30 },  // 25% 카나리
        { duration: '40s', target: 40 },  // 50% 카나리
        { duration: '40s', target: 50 },  // 100% 카나리 (최종)
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.02'],
    'response_time_by_version{version:legacy}': ['p(95)<800'],
    'response_time_by_version{version:canary}': ['p(95)<600'],
  },
};

export default function () {
  const baseUrl = 'http://localhost:8000';
  
  // 다양한 요청 패턴으로 실제 사용자 시뮬레이션
  const requestTypes = [
    { endpoint: '/api/test', weight: 60 },
    { endpoint: '/health', weight: 20 },
    { endpoint: '/api/metrics', weight: 15 },
    { endpoint: '/api/traffic/status', weight: 5 },
  ];
  
  const randomValue = Math.random() * 100;
  let selectedEndpoint = '/api/test';
  let cumulative = 0;
  
  for (const req of requestTypes) {
    cumulative += req.weight;
    if (randomValue <= cumulative) {
      selectedEndpoint = req.endpoint;
      break;
    }
  }

  const startTime = Date.now();
  const response = http.get(`${baseUrl}${selectedEndpoint}`, {
    headers: {
      'User-Agent': 'K6-TrafficShifting/1.0',
      'X-Test-Phase': 'traffic-shifting',
    },
  });
  
  const duration = Date.now() - startTime;
  const upstreamServer = response.headers['X-Upstream-Server'];
  const version = upstreamServer && upstreamServer.includes('legacy') ? 'legacy' : 'canary';

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time acceptable': (r) => r.timings.duration < 1000,
    'has canary headers': (r) => r.headers['X-Canary-Weight'] !== undefined,
  });

  // 버전별 메트릭 수집
  responseTimeByVersion.add(duration, { version: version });
  throughputByVersion.add(1, { version: version, endpoint: selectedEndpoint });
  
  // 실시간 트래픽 분산 로깅
  console.log(`${version.toUpperCase()}: ${selectedEndpoint} -> ${response.status} (${duration}ms)`);

  sleep(Math.random() * 0.5); // 0~500ms 랜덤 대기
}

export function handleSummary(data) {
  console.log('\n=== Phase 2: Traffic Shifting Results ===');
  console.log('Monitor these metrics in Grafana:');
  console.log('1. Traffic distribution:');
  console.log('   sum by (upstream_addr) (count_over_time({job="nginx", log_type="routing"}[5m]))');
  console.log('2. Response time comparison:');
  console.log('   histogram_quantile(0.95, sum(rate({job="nginx"}) by (le))');
  console.log('3. Error rate by version:');
  console.log('   rate({job="spring-boot"} |= "ERROR"[5m])');
  
  return {
    'traffic-shifting-summary.json': JSON.stringify(data, null, 2),
  };
}