import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 테스트 설정
export const options = {
  scenarios: {
    // 트래픽 컨트롤 API 테스트
    traffic_control: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: 'testTrafficControl',
    },
    // 백그라운드 트래픽 생성
    background_traffic: {
      executor: 'constant-vus',
      vus: 10,
      duration: '5m',
      exec: 'generateBackgroundTraffic',
      startTime: '10s', // 트래픽 컨트롤 테스트 후 시작
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';
const CONTROLLER_URL = __ENV.CONTROLLER_URL || 'http://localhost:8000';

// 트래픽 가중치 조정 시나리오
const trafficScenarios = new SharedArray('traffic scenarios', function () {
  return [
    { legacy: 100, refactored: 0, stage: 'Initial - Legacy Only' },
    { legacy: 95, refactored: 5, stage: 'Stage 1 - 5%' },
    { legacy: 90, refactored: 10, stage: 'Stage 2 - 10%' },
    { legacy: 75, refactored: 25, stage: 'Stage 3 - 25%' },
    { legacy: 50, refactored: 50, stage: 'Stage 4 - 50%' },
    { legacy: 0, refactored: 100, stage: 'Final - Refactored Only' },
    { legacy: 100, refactored: 0, stage: 'Rollback - Legacy Only' },
  ];
});

// 트래픽 컨트롤 API 테스트
export function testTrafficControl() {
  console.log('=== 트래픽 컨트롤 API 테스트 시작 ===');
  
  // 1. 현재 상태 확인
  console.log('1. 현재 트래픽 상태 확인');
  let response = http.get(`${CONTROLLER_URL}/api/traffic/status`);
  check(response, {
    'status is 200': (r) => r.status === 200,
    'has legacy weight': (r) => JSON.parse(r.body).legacyWeight !== undefined,
    'has refactored weight': (r) => JSON.parse(r.body).refactoredWeight !== undefined,
  });
  console.log(`현재 상태: ${response.body}`);
  
  // 2. 카나리 배포 상태 확인
  console.log('2. 카나리 배포 상태 확인');
  response = http.get(`${CONTROLLER_URL}/api/traffic/canary/status`);
  check(response, {
    'canary status is 200': (r) => r.status === 200,
    'has deployment status': (r) => JSON.parse(r.body).status !== undefined,
  });
  console.log(`카나리 상태: ${response.body}`);
  
  // 3. 메트릭 상태 확인
  console.log('3. 메트릭 상태 확인');
  response = http.get(`${CONTROLLER_URL}/api/metrics/health`);
  check(response, {
    'metrics health is 200': (r) => r.status === 200,
    'has health status': (r) => JSON.parse(r.body).isHealthy !== undefined,
  });
  console.log(`메트릭 상태: ${response.body}`);
  
  // 4. 각 단계별 트래픽 가중치 조정 테스트
  console.log('4. 단계별 트래픽 가중치 조정 테스트');
  for (let i = 0; i < trafficScenarios.length; i++) {
    const scenario = trafficScenarios[i];
    console.log(`\n--- ${scenario.stage} ---`);
    
    // 트래픽 가중치 조정
    const payload = JSON.stringify({
      legacyWeight: scenario.legacy,
      refactoredWeight: scenario.refactored
    });
    
    response = http.post(`${CONTROLLER_URL}/api/traffic/adjust`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    
    check(response, {
      [`${scenario.stage} - adjust success`]: (r) => r.status === 200,
      [`${scenario.stage} - correct weights`]: (r) => {
        const body = JSON.parse(r.body);
        return body.legacyWeight === scenario.legacy && 
               body.refactoredWeight === scenario.refactored;
      },
    });
    
    console.log(`조정 결과: ${response.body}`);
    
    // 트래픽 분산 확인을 위한 요청들
    const testRequests = 20;
    let legacyCount = 0;
    let refactoredCount = 0;
    
    console.log(`${testRequests}개 요청으로 트래픽 분산 확인 중...`);
    for (let j = 0; j < testRequests; j++) {
      const testResponse = http.get(`${BASE_URL}/api/hello`);
      if (testResponse.status === 200) {
        const body = JSON.parse(testResponse.body);
        if (body.version === 'legacy') {
          legacyCount++;
        } else if (body.version === 'refactored') {
          refactoredCount++;
        }
      }
    }
    
    const actualLegacyPercent = (legacyCount / testRequests) * 100;
    const actualRefactoredPercent = (refactoredCount / testRequests) * 100;
    
    console.log(`실제 분산: Legacy ${actualLegacyPercent.toFixed(1)}%, Refactored ${actualRefactoredPercent.toFixed(1)}%`);
    console.log(`목표 분산: Legacy ${scenario.legacy}%, Refactored ${scenario.refactored}%`);
    
    // 다음 단계로 넘어가기 전 대기
    sleep(3);
  }
  
  // 5. 카나리 배포 자동화 테스트
  console.log('\n5. 카나리 배포 자동화 테스트');
  
  // 카나리 배포 시작
  response = http.post(`${CONTROLLER_URL}/api/traffic/canary/start`);
  check(response, {
    'canary start success': (r) => r.status === 200,
    'deployment started': (r) => JSON.parse(r.body).status === 'DEPLOYING',
  });
  console.log(`카나리 배포 시작: ${response.body}`);
  
  sleep(2);
  
  // 다음 단계 진행
  response = http.post(`${CONTROLLER_URL}/api/traffic/canary/next-stage`);
  check(response, {
    'canary next stage success': (r) => r.status === 200,
  });
  console.log(`다음 단계 진행: ${response.body}`);
  
  sleep(2);
  
  // 롤백 테스트
  response = http.post(`${CONTROLLER_URL}/api/traffic/canary/rollback`);
  check(response, {
    'canary rollback success': (r) => r.status === 200,
    'deployment stable': (r) => JSON.parse(r.body).status === 'STABLE',
  });
  console.log(`롤백 실행: ${response.body}`);
  
  // 6. 에러 처리 테스트
  console.log('\n6. 에러 처리 테스트');
  
  // 잘못된 가중치 (합이 100이 아님)
  const invalidPayload = JSON.stringify({
    legacyWeight: 60,
    refactoredWeight: 30
  });
  
  response = http.post(`${CONTROLLER_URL}/api/traffic/adjust`, invalidPayload, {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(response, {
    'invalid weights rejected': (r) => r.status === 400,
    'error message present': (r) => r.body.includes('must sum to 100'),
  });
  console.log(`잘못된 가중치 테스트: ${response.status} - ${response.body}`);
  
  console.log('\n=== 트래픽 컨트롤 API 테스트 완료 ===');
}

// 백그라운드 트래픽 생성
export function generateBackgroundTraffic() {
  const endpoints = [
    '/api/hello',
    '/api/test',
    '/api/version',
    '/api/error-simulation'
  ];
  
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  
  const response = http.get(`${BASE_URL}${endpoint}`, {
    tags: { endpoint: endpoint },
  });
  
  check(response, {
    'background traffic success': (r) => r.status === 200 || r.status === 500,
  });
  
  // 랜덤 지연 (0.5초 ~ 2초)
  sleep(0.5 + Math.random() * 1.5);
}