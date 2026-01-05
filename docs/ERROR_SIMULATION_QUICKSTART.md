# 에러 시뮬레이션 빠른 시작 가이드

## 5분 안에 시작하기

### 1. 애플리케이션 시작

```bash
# 빌드
./gradlew build

# Docker Compose로 전체 스택 시작
docker-compose up -d

# 또는 로컬에서만 실행
./gradlew bootRun
```

### 2. 기본 에러 시뮬레이션 활성화

```bash
# API 에러 활성화 (10% 확률)
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=true"

# 에러율 확인
curl http://localhost:8080/api/error-simulation/config
```

### 3. 에러 발생시키기

```bash
# 방법 1: 랜덤 에러 (여러 번 호출)
for i in {1..20}; do
  curl http://localhost:8080/api/error-simulation/random
  echo ""
done

# 방법 2: 특정 에러 타입
curl http://localhost:8080/api/error-simulation/type/INTERNAL_SERVER_ERROR
```

### 4. 모니터링 확인

#### Grafana (추천)
1. http://localhost:3000 접속
2. admin/admin으로 로그인
3. 대시보드에서 에러 확인

#### Prometheus
1. http://localhost:9090 접속
2. 다음 쿼리 실행:
```promql
rate(simulated_errors_total[5m])
```

#### 로그 파일
```bash
# 에러 로그만 보기
tail -f logs/error.log

# 에러 시뮬레이션 전용 로그
tail -f logs/error-simulation.log

# 전체 애플리케이션 로그
tail -f logs/app.log
```

### 5. 통계 확인

```bash
# 에러 발생 통계
curl http://localhost:8080/api/error-simulation/statistics

# 결과 예시:
# {
#   "totalErrorsSimulated": 42,
#   "errorsByType": {
#     "INTERNAL_SERVER_ERROR": 15,
#     "DATABASE_ERROR": 8,
#     ...
#   }
# }
```

## 주요 시나리오

### 시나리오 1: 간헐적 에러 발생 (실전 시뮬레이션)

```bash
# 1. 낮은 에러율로 시작 (5%)
curl -X POST "http://localhost:8080/api/error-simulation/rate?apiRate=5"
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=true"

# 2. 정상 트래픽 + 간헐적 에러
for i in {1..100}; do
  curl http://localhost:8080/api/error-simulation/random
  sleep 0.5
done

# 3. Grafana에서 에러율 확인
```

### 시나리오 2: 주기적 에러 발생 (백그라운드)

```bash
# 30초마다 자동으로 에러 발생
curl -X POST "http://localhost:8080/api/error-simulation/periodic/toggle?enabled=true"

# 실시간 로그 확인
tail -f logs/error-simulation.log
```

### 시나리오 3: 특정 에러 집중 테스트

```bash
# 데이터베이스 에러만 20회 발생
for i in {1..20}; do
  curl http://localhost:8080/api/error-simulation/type/DATABASE_ERROR
  sleep 1
done

# Prometheus에서 특정 에러 타입 확인
# 쿼리: simulated_errors_total{error_type="database_error"}
```

### 시나리오 4: 고부하 에러 상황

```bash
# 1. 높은 에러율 설정 (50%)
curl -X POST "http://localhost:8080/api/error-simulation/rate?apiRate=50"
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=true"

# 2. K6로 부하 테스트 (있는 경우)
k6 run k6-tests/phase1-error-simulation.js

# 3. Grafana 대시보드에서 실시간 모니터링
```

## 자주 사용하는 명령어

```bash
# 현재 설정 보기
curl http://localhost:8080/api/error-simulation/config

# 사용 가능한 에러 타입 보기
curl http://localhost:8080/api/error-simulation/types

# 통계 확인
curl http://localhost:8080/api/error-simulation/statistics

# 통계 초기화
curl -X POST http://localhost:8080/api/error-simulation/statistics/reset

# 모든 에러 비활성화
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=false"
curl -X POST "http://localhost:8080/api/error-simulation/periodic/toggle?enabled=false"
```

## 팁

1. **로그 레벨별 분리**
   - ERROR만 보려면: `tail -f logs/error.log`
   - 모든 로그: `tail -f logs/app.log`
   - 시뮬레이션만: `tail -f logs/error-simulation.log`

2. **Grafana 대시보드 추천 패널**
   - 에러율 타임시리즈
   - 에러 타입 파이 차트
   - HTTP 상태 코드 분포

3. **Prometheus 쿼리 예제**
   ```promql
   # 전체 에러 발생률
   rate(simulated_errors_total[5m])

   # 에러 타입별 발생률
   sum by (error_type) (rate(simulated_errors_total[5m]))

   # 5xx 에러만
   sum(rate(simulated_errors_total{http_status=~"5.."}[5m]))
   ```

4. **로그 검색 (grep)**
   ```bash
   # 특정 에러 타입만
   grep "INTERNAL_SERVER_ERROR" logs/error-simulation.log

   # 최근 ERROR 로그
   grep "ERROR" logs/app.log | tail -20
   ```

## 문제 해결

**에러가 발생하지 않아요**
```bash
# 1. 설정 확인
curl http://localhost:8080/api/error-simulation/config

# 2. API 에러 활성화 확인
# periodicErrorEnabled 또는 apiErrorEnabled가 true인지 확인

# 3. 에러율 높이기
curl -X POST "http://localhost:8080/api/error-simulation/rate?apiRate=50"
```

**로그가 안 보여요**
```bash
# 1. logs 디렉토리 생성
mkdir -p logs

# 2. 애플리케이션 재시작
./gradlew bootRun

# 3. 로그 파일 확인
ls -la logs/
```

**Prometheus 메트릭이 안 보여요**
```bash
# 1. Actuator 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus | grep simulated

# 2. Prometheus 설정 확인
curl http://localhost:9090/api/v1/targets

# 3. 에러 발생시킨 후 확인
curl http://localhost:8080/api/error-simulation/random
curl http://localhost:8080/actuator/prometheus | grep simulated
```

## 다음 단계

- 상세 가이드: [ERROR_SIMULATION_GUIDE.md](ERROR_SIMULATION_GUIDE.md)
- 전체 프로젝트 문서: [../CLAUDE.md](../CLAUDE.md)
- K6 테스트: [../k6-tests/README.md](../k6-tests/README.md)
