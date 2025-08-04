import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// 커스텀 메트릭 정의
const autoDeploymentRequests = new Counter('auto_deployment_requests');
const businessConversions = new Counter('business_conversions');
const sloViolations = new Counter('slo_violations');
const deploymentStageChanges = new Counter('deployment_stage_changes');
const businessConversionRate = new Rate('business_conversion_rate');
const currentDeploymentStage = new Gauge('current_deployment_stage');

export const options = {
  scenarios: {
    // 자동 배포 모니터링 시나리오
    auto_deployment_monitor: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      exec: 'monitorAutoDeployment',
    },
    
    // 일반 트래픽 시뮬레이션
    normal_traffic: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '2m', target: 10 },
        { duration: '8m', target: 15 },
        { duration: '2m', target: 5 },
      ],
      exec: 'simulateNormalTraffic',
    },
    
    // 비즈니스 트랜잭션 시뮬레이션
    business_transactions: {
      executor: 'constant-vus',
      vus: 3,
      duration: '12m',
      exec: 'simulateBusinessTransactions',
    },
  },
  
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    business_conversion_rate: ['rate>0.85'],
    slo_violations: ['count<5'],
  },
};

const BASE_URL = 'http://localhost:8000';

/**
 * 자동 배포 모니터링 및 관리
 */
export function monitorAutoDeployment() {
  console.log('🚀 Starting Auto Deployment Monitor');
  
  // 1. 자동 배포 시작
  const startResponse = http.post(`${BASE_URL}/api/auto-deployment/start`);
  const startSuccess = check(startResponse, {
    'auto deployment started': (r) => r.status === 200 && JSON.parse(r.body).success,
  });
  
  if (!startSuccess) {
    console.log('❌ Failed to start auto deployment');
    return;
  }
  
  console.log('✅ Auto deployment started successfully');
  
  // 2. 배포 진행 상황 모니터링 (10분간)
  for (let i = 0; i < 60; i++) { // 10초마다 체크, 총 10분
    sleep(10);
    
    // 배포 상태 확인
    const statusResponse = http.get(`${BASE_URL}/api/auto-deployment/status`);
    autoDeploymentRequests.add(1);
    
    if (statusResponse.status === 200) {
      const status = JSON.parse(statusResponse.body);
      
      // 현재 배포 단계 기록
      if (status.deploymentStatus && status.deploymentStatus.currentStage !== undefined) {
        currentDeploymentStage.add(status.deploymentStatus.currentStage);
        console.log(`📊 Current deployment stage: ${status.deploymentStatus.currentStage + 1}/${status.deploymentStatus.totalStages}`);
      }
      
      // SLI/SLO 상태 확인
      const sliSloResponse = http.get(`${BASE_URL}/api/auto-deployment/sli-slo`);
      if (sliSloResponse.status === 200) {
        const sliSlo = JSON.parse(sliSloResponse.body);
        
        if (!sliSlo.sloCompliant) {
          sloViolations.add(1);
          console.log(`⚠️ SLO violation detected: Error rate: ${sliSlo.errorRatePercent}%, Response time: ${sliSlo.responseTimeP95}s`);
        }
        
        console.log(`📈 Current metrics: Error rate: ${sliSlo.errorRatePercent?.toFixed(2)}%, P95: ${sliSlo.responseTimeP95?.toFixed(2)}s, Samples: ${sliSlo.sampleSize}`);
      }
      
      // 배포 완료 또는 실패시 종료
      if (!status.inProgress) {
        if (status.deploymentStatus && status.deploymentStatus.status === 'STABLE' && 
            status.deploymentStatus.currentStage >= status.deploymentStatus.totalStages) {
          console.log('🎉 Auto deployment completed successfully!');
        } else {
          console.log(`❌ Auto deployment failed or stopped: ${status.deploymentStatus?.status}`);
        }
        break;
      }
    }
  }
  
  console.log('🔚 Auto deployment monitoring completed');
}

/**
 * 일반 트래픽 시뮬레이션
 */
export function simulateNormalTraffic() {
  const scenarios = [
    { endpoint: '/api/test', weight: 40 },
    { endpoint: '/api/hello', weight: 30 },
    { endpoint: '/api/version', weight: 20 },
    { endpoint: '/api/error-simulation', weight: 10 },
  ];
  
  // 가중치 기반 엔드포인트 선택
  const rand = Math.random() * 100;
  let cumulative = 0;
  let selectedEndpoint = '/api/test';
  
  for (const scenario of scenarios) {
    cumulative += scenario.weight;
    if (rand <= cumulative) {
      selectedEndpoint = scenario.endpoint;
      break;
    }
  }
  
  const response = http.get(`${BASE_URL}${selectedEndpoint}`, {
    headers: {
      'User-Agent': 'K6-AutoDeployment-Test/1.0',
      'X-Test-Scenario': 'normal-traffic',
    },
  });
  
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has upstream header': (r) => r.headers['X-Upstream-Server'] !== undefined,
  });
  
  // 잠시 대기 (실제 사용자 패턴 시뮬레이션)
  sleep(Math.random() * 3 + 1); // 1-4초 대기
}

/**
 * 비즈니스 트랜잭션 시뮬레이션
 */
export function simulateBusinessTransactions() {
  const transactionTypes = ['order', 'signup', 'login'];
  const transactionType = transactionTypes[Math.floor(Math.random() * transactionTypes.length)];
  
  let response;
  let conversionSuccess = false;
  
  switch (transactionType) {
    case 'order':
      response = http.post(`${BASE_URL}/api/business/order`, JSON.stringify({
        amount: Math.floor(Math.random() * 200) + 50,
        items: Math.floor(Math.random() * 5) + 1,
        userId: `user-${Math.floor(Math.random() * 1000)}`,
      }), {
        headers: { 'Content-Type': 'application/json' },
      });
      
      if (response.status === 200) {
        const body = JSON.parse(response.body);
        conversionSuccess = body.success === true;
      }
      break;
      
    case 'signup':
      response = http.post(`${BASE_URL}/api/business/signup`, JSON.stringify({
        email: `user${Math.floor(Math.random() * 10000)}@example.com`,
        username: `user${Math.floor(Math.random() * 10000)}`,
        referralCode: Math.random() > 0.7 ? 'FRIEND2024' : null,
      }), {
        headers: { 'Content-Type': 'application/json' },
      });
      
      if (response.status === 200) {
        const body = JSON.parse(response.body);
        conversionSuccess = body.success === true;
      }
      break;
      
    case 'login':
      response = http.post(`${BASE_URL}/api/business/login`, JSON.stringify({
        username: `user${Math.floor(Math.random() * 100)}`,
        password: 'password123',
      }), {
        headers: { 'Content-Type': 'application/json' },
      });
      
      if (response.status === 200) {
        const body = JSON.parse(response.body);
        conversionSuccess = body.success === true;
      }
      break;
  }
  
  // 비즈니스 메트릭 기록
  businessConversions.add(1);
  businessConversionRate.add(conversionSuccess);
  
  check(response, {
    'business transaction status is 200': (r) => r.status === 200,
    'business transaction successful': () => conversionSuccess,
  });
  
  // A/B 테스트 기능 플래그 확인 (가끔)
  if (Math.random() > 0.8) {
    const features = ['checkout_optimization', 'personalization', 'new_dashboard'];
    const feature = features[Math.floor(Math.random() * features.length)];
    const userGroups = ['default', 'premium', 'beta'];
    const userGroup = userGroups[Math.floor(Math.random() * userGroups.length)];
    
    const featureResponse = http.get(`${BASE_URL}/api/business/feature/${feature}?userGroup=${userGroup}`);
    
    check(featureResponse, {
      'feature flag response is 200': (r) => r.status === 200,
      'feature flag has config': (r) => {
        if (r.status === 200) {
          const body = JSON.parse(r.body);
          return body.enabled !== undefined;
        }
        return false;
      },
    });
  }
  
  sleep(Math.random() * 5 + 2); // 2-7초 대기
}

/**
 * 테스트 시작시 실행
 */
export function setup() {
  console.log('🔧 Setting up enhanced auto deployment test');
  
  // 초기 상태 확인
  const dashboardResponse = http.get(`${BASE_URL}/api/auto-deployment/dashboard`);
  if (dashboardResponse.status === 200) {
    const dashboard = JSON.parse(dashboardResponse.body);
    console.log(`📊 Initial state: Auto deployment enabled: ${dashboard.autoDeployment.enabled}, SLO compliant: ${dashboard.sliSlo.sloCompliant}`);
  }
  
  return {};
}

/**
 * 테스트 종료시 실행  
 */
export function teardown(data) {
  console.log('🧹 Tearing down enhanced auto deployment test');
  
  // 최종 상태 확인
  const dashboardResponse = http.get(`${BASE_URL}/api/auto-deployment/dashboard`);
  if (dashboardResponse.status === 200) {
    const dashboard = JSON.parse(dashboardResponse.body);
    console.log(`📊 Final state: Deployment in progress: ${dashboard.autoDeployment.inProgress}, Final SLO compliance: ${dashboard.sliSlo.sloCompliant}`);
  }
  
  // 비즈니스 메트릭 요약
  const businessSummaryResponse = http.get(`${BASE_URL}/api/business/metrics/summary`);
  if (businessSummaryResponse.status === 200) {
    const summary = JSON.parse(businessSummaryResponse.body);
    console.log(`💼 Business metrics: Order conversion: ${summary.orderConversionRate?.toFixed(2)}%, Signup conversion: ${summary.signupConversionRate?.toFixed(2)}%`);
  }
}