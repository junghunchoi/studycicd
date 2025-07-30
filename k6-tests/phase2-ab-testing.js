import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// A/B 테스트 메트릭
const conversionRate = new Rate('conversion_rate');
const businessMetric = new Counter('business_metric');
const userSatisfaction = new Trend('user_satisfaction');

export const options = {
  scenarios: {
    // A/B 테스트 시뮬레이션
    ab_testing: {
      executor: 'constant-vus',
      vus: 25,
      duration: '10m',
    },
  },
};

// 사용자 행동 시뮬레이션
const userProfiles = [
  { type: 'mobile', behavior: 'quick', satisfaction_threshold: 300 },
  { type: 'desktop', behavior: 'thorough', satisfaction_threshold: 500 },
  { type: 'tablet', behavior: 'mixed', satisfaction_threshold: 400 },
];

export default function () {
  const baseUrl = 'http://localhost:8000';
  const userProfile = userProfiles[Math.floor(Math.random() * userProfiles.length)];
  
  // 사용자 세션 시뮬레이션
  const sessionId = `session_${Math.random().toString(36).substr(2, 9)}`;
  
  // 사용자 여정 시뮬레이션
  const userJourney = [
    { endpoint: '/health', action: 'landing' },
    { endpoint: '/api/test', action: 'browse' },
    { endpoint: '/api/metrics', action: 'interact' },
  ];
  
  let sessionSuccess = true;
  let totalResponseTime = 0;
  
  for (const step of userJourney) {
    const response = http.get(`${baseUrl}${step.endpoint}`, {
      headers: {
        'User-Agent': `K6-${userProfile.type}/1.0`,
        'X-Session-ID': sessionId,
        'X-User-Type': userProfile.type,
        'X-Test-Scenario': 'ab-testing',
      },
    });
    
    const stepSuccess = check(response, {
      'status is 200': (r) => r.status === 200,
      'response time acceptable': (r) => r.timings.duration < userProfile.satisfaction_threshold,
    });
    
    if (!stepSuccess) {
      sessionSuccess = false;
    }
    
    totalResponseTime += response.timings.duration;
    
    // 업스트림 서버 정보
    const upstreamServer = response.headers['X-Upstream-Server'];
    const version = upstreamServer && upstreamServer.includes('legacy') ? 'legacy' : 'canary';
    
    // 비즈니스 메트릭 수집
    businessMetric.add(1, { 
      version: version, 
      user_type: userProfile.type, 
      action: step.action,
      success: stepSuccess 
    });
    
    console.log(`${sessionId}: ${step.action} on ${version} -> ${response.status} (${response.timings.duration.toFixed(0)}ms)`);
    
    // 사용자 행동에 따른 대기 시간
    if (userProfile.behavior === 'quick') {
      sleep(0.1);
    } else if (userProfile.behavior === 'thorough') {
      sleep(0.5);
    } else {
      sleep(Math.random() * 0.4);
    }
  }
  
  // 세션 결과 평가
  conversionRate.add(sessionSuccess, { user_type: userProfile.type });
  userSatisfaction.add(totalResponseTime, { user_type: userProfile.type });
  
  // 세션 완료 로깅
  console.log(`Session ${sessionId} (${userProfile.type}): ${sessionSuccess ? 'SUCCESS' : 'FAILED'} - Total: ${totalResponseTime.toFixed(0)}ms`);
  
  sleep(1); // 세션 간 간격
}

export function handleSummary(data) {
  const conversionData = data.metrics.conversion_rate?.values || {};
  
  console.log('\n=== Phase 2.2: A/B Testing Results ===');
  console.log(`Overall Conversion Rate: ${(conversionData.rate * 100).toFixed(2)}%`);
  console.log('\nBusiness Metrics Analysis:');
  console.log('1. User satisfaction by version:');
  console.log('   {job="spring-boot"} |~ "Session.*SUCCESS|FAILED"');
  console.log('2. Performance by user type:');
  console.log('   {job="nginx"} |~ "X-User-Type"');
  console.log('3. Conversion funnel:');
  console.log('   sum by (version) (count_over_time({job="nginx"} |~ "action"[5m]))');
  
  return {
    'ab-testing-summary.json': JSON.stringify(data, null, 2),
  };
}