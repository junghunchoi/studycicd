# 카나리 배포 학습용 K6 테스트 스크립트

이 디렉토리는 카나리 배포를 완전히 습득하기 위한 단계별 학습 시나리오를 제공합니다.

## 📚 학습 단계별 구성

### Phase 1: 기본 카나리 배포 이해
- `phase1-basic-canary.js`: 기본 트래픽 분배 (Legacy 95%, Canary 5%)
- `phase1-error-simulation.js`: 에러 상황 시뮬레이션 및 LogQL 디버깅

### Phase 2: 트래픽 관리 및 A/B 테스트
- `phase2-traffic-shifting.js`: 점진적 트래픽 전환 (5% → 10% → 25% → 50% → 100%)
- `phase2-ab-testing.js`: 사용자 세그먼트별 A/B 테스트

### Phase 3: 자동화된 장애 대응
- `phase3-auto-rollback.js`: SLO 위반 시 자동 롤백 시뮬레이션

### Phase 4: 고급 배포 패턴
- `phase4-multi-canary.js`: 다중 카나리 버전 동시 운영

### Phase 5: 프로덕션 규모 테스트
- `phase5-production-load.js`: 대용량 트래픽 및 실제 사용자 패턴 시뮬레이션

## 🚀 실행 방법

### 전체 학습 과정 실행
```bash
./run-all-phases.sh
```

### 개별 Phase 실행
```bash
# Phase 1: 기본 카나리 배포
k6 run phase1-basic-canary.js

# Phase 2: 트래픽 전환
k6 run phase2-traffic-shifting.js

# Phase 3: 자동 롤백
k6 run phase3-auto-rollback.js

# Phase 4: 다중 카나리
k6 run phase4-multi-canary.js

# Phase 5: 프로덕션 부하
k6 run phase5-production-load.js
```

## 📊 모니터링 및 분석

### Grafana 대시보드
- URL: http://localhost:3000
- 계정: admin/admin
- 대시보드: "Canary Deployment Logs"

### 주요 LogQL 쿼리

#### 1. 트래픽 분배 확인
```logql
sum by (upstream_addr) (count_over_time({job="nginx", log_type="routing"}[5m]))
```

#### 2. 버전별 에러율
```logql
(count_over_time({job="spring-boot", version="canary"} |= "ERROR"[5m]) / 
 count_over_time({job="spring-boot", version="canary"}[5m])) * 100
```

#### 3. Request ID 추적
```logql
{job=~"nginx|spring-boot"} |~ "req-abc123"
```

#### 4. 응답 시간 분석
```logql
histogram_quantile(0.95, 
  sum(rate({job="nginx"} | regex "request_time=(?P<time>[0-9.]+)"[5m])) by (le))
```

#### 5. 자동 롤백 트리거
```logql
{job="nginx"} |~ "ROLLBACK TRIGGERED"
```

## 🎯 학습 목표별 체크리스트

### Phase 1 완료 후
- [ ] NGINX 트래픽 분배 메커니즘 이해
- [ ] LogQL 기본 쿼리 작성 가능
- [ ] Request ID 기반 요청 추적 가능
- [ ] 에러 패턴 분석 및 디버깅 가능

### Phase 2 완료 후
- [ ] 점진적 트래픽 전환 전략 이해
- [ ] SLI/SLO 정의 및 측정 방법 습득
- [ ] A/B 테스트 결과 분석 가능
- [ ] 비즈니스 메트릭 기반 의사결정 가능

### Phase 3 완료 후
- [ ] 자동 롤백 트리거 조건 이해
- [ ] MTTR(복구 시간) 측정 방법 습득
- [ ] 장애 감지 및 대응 자동화 구현 가능

### Phase 4 완료 후
- [ ] 다중 버전 동시 운영 전략 이해
- [ ] 사용자 세그먼트 기반 라우팅 구현 가능
- [ ] 기능 플래그와 카나리 배포 연동 가능

### Phase 5 완료 후
- [ ] 프로덕션 규모 부하 처리 경험
- [ ] 실제 사용자 패턴 시뮬레이션 가능
- [ ] 대규모 시스템 모니터링 및 최적화 가능

---

이 학습 과정을 완료하면 카나리 배포의 모든 측면을 실무에 적용할 수 있는 수준이 됩니다!