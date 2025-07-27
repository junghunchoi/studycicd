# K6 테스트 스크립트

트래픽 컨트롤러 API를 테스트하기 위한 K6 스크립트 모음입니다.

## 사전 준비

### 1. K6 설치
```bash
# macOS
brew install k6

# Linux (Ubuntu/Debian)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### 2. 서비스 시작
```bash
# 프로젝트 루트에서
docker-compose up -d
```

## 테스트 스크립트

### 1. `traffic-control-test.js`
**트래픽 컨트롤 API 기능 테스트**
- API 엔드포인트 동작 확인
- 트래픽 가중치 조정 테스트
- 카나리 배포 자동화 테스트
- 에러 처리 검증

**실행:**
```bash
k6 run traffic-control-test.js
```

### 2. `canary-deployment-test.js`
**카나리 배포 시뮬레이션**
- 실제 카나리 배포 프로세스 시뮬레이션
- 단계별 트래픽 증가 (5% → 10% → 25% → 50% → 100%)
- 백그라운드 트래픽 생성
- 메트릭 모니터링

**실행:**
```bash
k6 run canary-deployment-test.js
```

### 3. `load-test.js`
**부하 테스트**
- 점진적 부하 증가 시나리오
- 스파이크 테스트
- 성능 임계값 검증
- 커스텀 메트릭 수집

**실행:**
```bash
k6 run load-test.js
```

### 4. `work-endpoint-test.js`
**Work 엔드포인트 지속 테스트**
- `/canary/work` 엔드포인트에 초당 10건 지속 요청
- 2분간 일정한 부하 유지
- 응답시간 및 성공률 모니터링
- 실시간 메트릭 수집

**실행:**
```bash
k6 run work-endpoint-test.js
```

**환경변수 설정:**
```bash
# 다른 호스트로 테스트하는 경우
k6 run --env BASE_URL="http://your-server:8080" work-endpoint-test.js
```

## 간편 실행

### 대화형 실행
```bash
./run-tests.sh
```

이 스크립트는 메뉴를 제공하여 원하는 테스트를 선택적으로 실행할 수 있습니다.

### 직접 실행
```bash
# 환경변수 설정 (옵션)
export BASE_URL="http://localhost"
export CONTROLLER_URL="http://localhost:8080"

# 개별 테스트 실행
k6 run --env BASE_URL="$BASE_URL" --env CONTROLLER_URL="$CONTROLLER_URL" traffic-control-test.js
```

## 테스트 시나리오

### 1. 트래픽 컨트롤 API 테스트
1. 현재 트래픽 상태 확인
2. 카나리 배포 상태 확인
3. 메트릭 상태 확인
4. 단계별 트래픽 가중치 조정
5. 카나리 배포 자동화 테스트
6. 에러 처리 테스트

### 2. 카나리 배포 시뮬레이션
1. 카나리 배포 시작 (5%)
2. 2분 대기 후 다음 단계 (10%)
3. 2분 대기 후 다음 단계 (25%)
4. 메트릭 확인 후 진행/롤백 결정
5. 최종 단계까지 진행 또는 롤백

### 3. 부하 테스트
1. **점진적 부하 증가:**
   - 0 → 20 VU (2분)
   - 20 VU 유지 (5분)
   - 20 → 50 VU (2분)
   - 50 VU 유지 (5분)
   - 50 → 0 VU (2분)

2. **스파이크 테스트:**
   - 0 → 100 VU (30초)
   - 100 VU 유지 (1분)
   - 100 → 0 VU (30초)

### 4. Work 엔드포인트 지속 테스트
1. **일정 부하 유지:**
   - 초당 10건 요청으로 2분간 지속
   - 실시간 성능 메트릭 수집
   - 에러율 및 응답시간 모니터링

2. **카나리 배포 중 안정성 검증:**
   - 트래픽 분산 중 Work 엔드포인트 안정성 확인
   - Legacy/Refactored 버전별 성능 비교
   - 롤백 시나리오에서의 영향 분석

## 성능 임계값

- **응답시간:** 95%의 요청이 2초 미만
- **에러율:** 5% 미만
- **가용성:** 서비스 중단 없음

## 모니터링

테스트 실행 중 다음 도구들로 모니터링할 수 있습니다:
- **Grafana:** http://localhost:3000 (admin/admin)
- **Prometheus:** http://localhost:9090
- **애플리케이션:** http://localhost

## 결과 분석

K6는 테스트 완료 후 다음과 같은 정보를 제공합니다:
- HTTP 요청 통계
- 응답시간 분포
- 에러율
- 커스텀 메트릭
- 체크 결과

이 데이터를 통해 시스템의 성능과 안정성을 검증할 수 있습니다.