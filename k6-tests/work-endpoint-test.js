import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
const workRequests = new Counter('work_requests_total');
const workFailures = new Rate('work_failure_rate');
const workDuration = new Trend('work_request_duration');

// 테스트 설정
export let options = {
  // 초당 10건 요청을 위한 설정
  scenarios: {
    constant_rate: {
      executor: 'constant-arrival-rate',
      rate: 10,        // 초당 10건
      timeUnit: '1s',  // 1초 단위
      duration: '10m',  // 2분간 실행
      preAllocatedVUs: 5,  // 미리 할당할 VU 수
      maxVUs: 20,      // 최대 VU 수
    },
  },
  
  // 성능 임계값 설정
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 미만
    http_req_failed: ['rate<0.05'],    // 에러율 5% 미만
    work_failure_rate: ['rate<0.05'],  // work 엔드포인트 에러율 5% 미만
    work_request_duration: ['p(95)<1500'], // work 요청의 95%가 1.5초 미만
  },
};

// 환경변수에서 Base URL 가져오기 (Docker Compose NGINX 포트 8000)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';

export default function() {
  // /work 엔드포인트에 GET 요청
  const response = http.get(`${BASE_URL}/canary/work`, {
    headers: {
      'User-Agent': 'k6-work-test/1.0',
      'Accept': 'application/json',
    },
    timeout: '30s',
  });

  // 응답 검증
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
    'body contains work ok': (r) => r.body.includes('work ok'),
    'no server errors': (r) => r.status < 500,
  });

  // 커스텀 메트릭 업데이트
  workRequests.add(1);
  workFailures.add(!success);
  workDuration.add(response.timings.duration);

  // 디버그 정보 출력 (10% 확률로)
  if (Math.random() < 0.1) {
    console.log(`Response status: ${response.status}, duration: ${response.timings.duration}ms`);
  }

  // 실패한 경우 에러 정보 출력
  if (!success) {
    console.error(`Request failed - Status: ${response.status}, Body: ${response.body}`);
  }
}

// 테스트 시작 시 실행
export function setup() {
  console.log('Starting work endpoint test...');
  console.log(`Target URL: ${BASE_URL}/canary/work`);
  console.log('Rate: 10 requests per second for 2 minutes');
  
  // 헬스체크로 서비스 가용성 확인
  const healthCheck = http.get(`${BASE_URL}/canary/health`);
  if (healthCheck.status !== 200) {
    throw new Error(`Service is not healthy: ${healthCheck.status}`);
  }
  
  return { startTime: new Date() };
}

// 테스트 종료 시 실행
export function teardown(data) {
  const endTime = new Date();
  const duration = (endTime - data.startTime) / 1000;
  console.log(`Test completed in ${duration} seconds`);
  console.log('Check Grafana dashboard for detailed metrics: http://localhost:3000');
}