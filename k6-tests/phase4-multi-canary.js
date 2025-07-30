import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 다중 카나리 배포 메트릭
const versionDistribution = new Counter('version_distribution');
const featureUsage = new Counter('feature_usage');
const abTestResults = new Rate('ab_test_results');

export const options = {
  scenarios: {
    // 다중 버전 동시 테스트
    multi_version_test: {
      executor: 'constant-vus',
      vus: 30,
      duration: '8m',
    },
  },
};

// 사용자 세그먼트 정의
const userSegments = [
  { 
    segment: 'premium', 
    features: ['new-ui', 'advanced-analytics'], 
    canary_probability: 0.3 
  },
  { 
    segment: 'standard', 
    features: ['new-ui'], 
    canary_probability: 0.1 
  },
  { 
    segment: 'basic', 
    features: [], 
    canary_probability: 0.05 
  },
];

export default function () {
  const baseUrl = 'http://localhost:8000';
  const userSegment = userSegments[Math.floor(Math.random() * userSegments.length)];
  const userId = `user_${Math.random().toString(36).substr(2, 8)}`;
  
  // 사용자 세그먼트에 따른 카나리 라우팅 시뮬레이션
  const headers = {
    'User-Agent': 'K6-MultiCanary/1.0',
    'X-User-ID': userId,
    'X-User-Segment': userSegment.segment,
    'X-Test-Scenario': 'multi-canary',
  };
  
  // 기능 플래그 시뮬레이션
  if (userSegment.features.length > 0) {
    headers['X-Feature-Flags'] = userSegment.features.join(',');
  }
  
  // A/B 테스트 버전 할당 (사용자 세그먼트 기반)
  const shouldUseCanary = Math.random() < userSegment.canary_probability;
  if (shouldUseCanary) {
    headers['X-Canary-User'] = 'true';
  }

  const response = http.get(`${baseUrl}/api/test`, { headers });
  const upstreamServer = response.headers['X-Upstream-Server'];
  const version = upstreamServer && upstreamServer.includes('legacy') ? 'legacy' : 'canary';
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'correct version routing': (r) => {
      if (shouldUseCanary) {
        return version === 'canary';
      }
      return true; // Legacy는 항상 OK
    },
    'feature flags respected': (r) => {
      // 기능 플래그가 있으면서 legacy 버전인 경우 체크
      return !(userSegment.features.length > 0 && version === 'legacy');
    },
  });
  
  // 메트릭 수집
  versionDistribution.add(1, { 
    version: version, 
    segment: userSegment.segment,
    canary_eligible: shouldUseCanary 
  });
  
  userSegment.features.forEach(feature => {
    featureUsage.add(1, { 
      feature: feature, 
      version: version, 
      segment: userSegment.segment 
    });
  });
  
  abTestResults.add(success, { 
    version: version, 
    segment: userSegment.segment 
  });
  
  console.log(`${userId} (${userSegment.segment}): ${version} -> ${response.status} | Features: [${userSegment.features.join(',')}]`);
  
  // 세그먼트별 다른 사용 패턴
  if (userSegment.segment === 'premium') {
    // Premium 사용자는 더 많은 기능 사용
    const advancedEndpoints = ['/api/analytics', '/api/reports', '/api/admin'];
    const advancedEndpoint = advancedEndpoints[Math.floor(Math.random() * advancedEndpoints.length)];
    
    const advancedResponse = http.get(`${baseUrl}${advancedEndpoint}`, headers);
    console.log(`  Premium feature: ${advancedEndpoint} -> ${advancedResponse.status}`);
    
    sleep(0.5); // Premium 사용자는 더 오래 머무름
  } else {
    sleep(0.2);
  }
}

export function handleSummary(data) {
  console.log('\n=== Phase 4.1: Multi-Canary Deployment Results ===');
  console.log('Version Distribution Analysis:');
  console.log('- Check user segment based routing');
  console.log('- Verify feature flag functionality');
  console.log('- Monitor A/B test performance');
  
  console.log('\nGrafana Monitoring Queries:');
  console.log('1. Segment-based routing:');
  console.log('   sum by (segment, version) (count_over_time({job="nginx"} |~ "X-User-Segment"[5m]))');
  console.log('2. Feature flag usage:');
  console.log('   {job="nginx"} |~ "X-Feature-Flags"');
  console.log('3. Canary eligibility vs actual routing:');
  console.log('   {job="nginx"} |~ "X-Canary-User.*true" |~ "upstream.*legacy"');
  
  return {
    'multi-canary-summary.json': JSON.stringify(data, null, 2),
  };
}