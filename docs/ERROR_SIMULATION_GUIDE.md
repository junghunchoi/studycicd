# 에러 시뮬레이션 가이드

## 개요

이 가이드는 Grafana와 Prometheus를 통한 애플리케이션 모니터링 학습을 위한 에러 시뮬레이션 시스템의 사용법을 설명합니다.

## 주요 기능

### 1. 다양한 에러 타입
시스템은 8가지 타입의 에러를 시뮬레이션할 수 있습니다:

- **INTERNAL_SERVER_ERROR (500)**: 내부 서버 에러
- **BAD_REQUEST (400)**: 잘못된 요청
- **NOT_FOUND (404)**: 리소스를 찾을 수 없음
- **SERVICE_UNAVAILABLE (503)**: 서비스 사용 불가
- **TIMEOUT (408)**: 요청 타임아웃
- **DATABASE_ERROR (500)**: 데이터베이스 에러
- **EXTERNAL_API_ERROR (502)**: 외부 API 에러
- **MEMORY_ERROR (500)**: 메모리 부족 에러

각 에러 타입은 5개 이상의 다양한 에러 메시지를 랜덤하게 사용합니다.

### 2. 에러 발생 방식

#### A. API 호출 시 에러 발생
- 설정된 확률에 따라 API 호출 시 랜덤 에러 발생
- 기본 에러율: 10%

#### B. 주기적 에러 발생
- 설정된 간격마다 자동으로 에러 발생
- 기본 간격: 30초
- 기본 에러율: 20%

### 3. 로깅
모든 에러는 다음 위치에 로깅됩니다:

- **콘솔**: 실시간 로그 확인
- **logs/app.log**: 전체 애플리케이션 로그
- **logs/error.log**: ERROR 레벨 이상의 로그만
- **logs/error-simulation.log**: 에러 시뮬레이션 전용 로그

### 4. Prometheus 메트릭
다음 메트릭이 자동으로 수집됩니다:

- `simulated_errors_total`: 에러 타입별 총 발생 횟수
  - 태그: `error_type`, `http_status`

## API 사용법

### 1. 랜덤 에러 발생

```bash
# 설정된 확률에 따라 랜덤 에러 발생
curl http://localhost:8080/api/error-simulation/random
```

### 2. 특정 타입 에러 발생

```bash
# 500 Internal Server Error 발생
curl http://localhost:8080/api/error-simulation/type/INTERNAL_SERVER_ERROR

# 404 Not Found 발생
curl http://localhost:8080/api/error-simulation/type/NOT_FOUND

# 데이터베이스 에러 발생
curl http://localhost:8080/api/error-simulation/type/DATABASE_ERROR
```

### 3. 설정 조회

```bash
# 현재 설정 확인
curl http://localhost:8080/api/error-simulation/config
```

응답 예시:
```json
{
  "periodicErrorEnabled": false,
  "periodicErrorIntervalSeconds": 30,
  "periodicErrorRate": 20,
  "apiErrorEnabled": true,
  "apiErrorRate": 10,
  "errorTypeWeights": {
    "internalError": 30,
    "badRequest": 15,
    "notFound": 10,
    "serviceUnavailable": 15,
    "timeout": 10,
    "databaseError": 10,
    "externalApiError": 5,
    "memoryError": 5
  }
}
```

### 4. 주기적 에러 활성화/비활성화

```bash
# 주기적 에러 활성화
curl -X POST "http://localhost:8080/api/error-simulation/periodic/toggle?enabled=true"

# 주기적 에러 비활성화
curl -X POST "http://localhost:8080/api/error-simulation/periodic/toggle?enabled=false"
```

### 5. API 에러 활성화/비활성화

```bash
# API 에러 활성화
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=true"

# API 에러 비활성화
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=false"
```

### 6. 에러율 설정

```bash
# 주기적 에러율 30%로 설정
curl -X POST "http://localhost:8080/api/error-simulation/rate?periodicRate=30"

# API 에러율 15%로 설정
curl -X POST "http://localhost:8080/api/error-simulation/rate?apiRate=15"

# 둘 다 설정
curl -X POST "http://localhost:8080/api/error-simulation/rate?periodicRate=30&apiRate=15"
```

### 7. 통계 조회

```bash
# 에러 발생 통계 확인
curl http://localhost:8080/api/error-simulation/statistics
```

응답 예시:
```json
{
  "totalErrorsSimulated": 127,
  "errorsByType": {
    "INTERNAL_SERVER_ERROR": 42,
    "BAD_REQUEST": 19,
    "NOT_FOUND": 13,
    "SERVICE_UNAVAILABLE": 18,
    "TIMEOUT": 12,
    "DATABASE_ERROR": 11,
    "EXTERNAL_API_ERROR": 7,
    "MEMORY_ERROR": 5
  },
  "timestamp": "2025-12-15T10:30:45"
}
```

### 8. 통계 초기화

```bash
# 통계 리셋
curl -X POST http://localhost:8080/api/error-simulation/statistics/reset
```

### 9. 사용 가능한 에러 타입 목록

```bash
# 모든 에러 타입과 메시지 확인
curl http://localhost:8080/api/error-simulation/types
```

## 학습 시나리오

### 시나리오 1: 기본 에러 모니터링

```bash
# 1. API 에러 활성화 (10% 확률)
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=true"

# 2. 부하 생성 (반복 호출)
for i in {1..100}; do
  curl http://localhost:8080/api/error-simulation/random
  sleep 0.1
done

# 3. Grafana에서 확인
# - http://localhost:3000
# - 대시보드에서 에러율 확인
# - Loki에서 에러 로그 확인

# 4. Prometheus에서 확인
# - http://localhost:9090
# - 쿼리: rate(simulated_errors_total[5m])
```

### 시나리오 2: 주기적 에러 발생

```bash
# 1. 주기적 에러 활성화 (30초마다, 20% 확률)
curl -X POST "http://localhost:8080/api/error-simulation/periodic/toggle?enabled=true"

# 2. 에러율 높이기
curl -X POST "http://localhost:8080/api/error-simulation/rate?periodicRate=50"

# 3. 로그 파일 모니터링
tail -f logs/error-simulation.log

# 4. 몇 분 후 통계 확인
curl http://localhost:8080/api/error-simulation/statistics
```

### 시나리오 3: 특정 에러 타입 집중 테스트

```bash
# 1. Database 에러만 발생
for i in {1..20}; do
  curl http://localhost:8080/api/error-simulation/type/DATABASE_ERROR
  sleep 1
done

# 2. Prometheus에서 확인
# 쿼리: simulated_errors_total{error_type="database_error"}

# 3. Grafana에서 에러 타입별 분포 확인
```

### 시나리오 4: 고부하 + 높은 에러율 시뮬레이션

```bash
# 1. 에러율 최대로 설정
curl -X POST "http://localhost:8080/api/error-simulation/rate?apiRate=50"
curl -X POST "http://localhost:8080/api/error-simulation/api/toggle?enabled=true"
curl -X POST "http://localhost:8080/api/error-simulation/periodic/toggle?enabled=true"

# 2. K6로 부하 테스트
k6 run k6-tests/phase1-error-simulation.js

# 3. Grafana 대시보드에서 실시간 모니터링
# - 에러율 급증 확인
# - 응답 시간 변화 확인
# - 로그 패턴 분석

# 4. 복구 시뮬레이션
curl -X POST "http://localhost:8080/api/error-simulation/rate?apiRate=5"
```

## Prometheus 쿼리 예제

```promql
# 전체 에러 발생률 (5분 기준)
rate(simulated_errors_total[5m])

# 에러 타입별 발생률
sum by (error_type) (rate(simulated_errors_total[5m]))

# HTTP 상태코드별 발생률
sum by (http_status) (rate(simulated_errors_total[5m]))

# 5xx 에러 비율
sum(rate(simulated_errors_total{http_status=~"5.."}[5m])) /
sum(rate(http_requests_total[5m])) * 100

# 가장 많이 발생한 에러 타입 (Top 3)
topk(3, sum by (error_type) (simulated_errors_total))
```

## Grafana 대시보드 패널 추천

### 1. 에러율 타임시리즈
- 쿼리: `rate(simulated_errors_total[5m])`
- 시각화: Time series
- 그룹: error_type

### 2. 에러 타입 분포
- 쿼리: `sum by (error_type) (simulated_errors_total)`
- 시각화: Pie chart

### 3. HTTP 상태 코드 분포
- 쿼리: `sum by (http_status) (simulated_errors_total)`
- 시각화: Bar gauge

### 4. 총 에러 카운트
- 쿼리: `sum(simulated_errors_total)`
- 시각화: Stat

### 5. 에러 로그
- 데이터소스: Loki
- 쿼리: `{job="app"} |= "Simulated" | json`
- 시각화: Logs

## LogQL 쿼리 예제 (Loki)

```logql
# 모든 시뮬레이션 에러 로그
{job="app"} |= "Simulated"

# ERROR 레벨만
{job="app"} |= "Simulated" |= "ERROR"

# 특정 에러 타입
{job="app"} |= "INTERNAL_SERVER_ERROR"

# 에러 발생률 (분당)
rate({job="app"} |= "Simulated" [1m])

# 에러 타입별 카운트
sum by (error_type) (count_over_time({job="app"} |= "Simulated" | json [5m]))
```

## 설정 파일

### application.properties

```properties
# Error Simulation Configuration
error-simulation.periodic.interval=30000          # 주기적 실행 간격 (ms)
error-simulation.periodic.enabled=false           # 주기적 에러 활성화 여부
error-simulation.periodic.error-rate=20           # 주기적 에러 발생 확률 (%)
error-simulation.api.enabled=false                # API 에러 활성화 여부
error-simulation.api.error-rate=10                # API 에러 발생 확률 (%)
```

## 문제 해결

### 에러가 발생하지 않는 경우

1. API 에러가 활성화되어 있는지 확인:
```bash
curl http://localhost:8080/api/error-simulation/config
```

2. 에러율이 너무 낮은지 확인 (최소 10% 권장)

3. 로그 확인:
```bash
tail -f logs/error-simulation.log
```

### 메트릭이 보이지 않는 경우

1. Prometheus가 정상 작동하는지 확인:
```bash
curl http://localhost:9090/-/healthy
```

2. 메트릭 엔드포인트 확인:
```bash
curl http://localhost:8080/actuator/prometheus | grep simulated_errors
```

### 로그 파일이 생성되지 않는 경우

1. logs 디렉토리가 있는지 확인:
```bash
mkdir -p logs
```

2. 권한 확인:
```bash
chmod 755 logs
```

## 고급 기능

### 에러 타입별 가중치 조정

ErrorSimulationConfig 클래스에서 각 에러 타입의 발생 빈도를 조정할 수 있습니다:

```java
config.setInternalErrorWeight(40);  // Internal Error를 더 자주 발생
config.setDatabaseErrorWeight(5);   // Database Error를 덜 발생
```

### 커스텀 에러 메시지 추가

ErrorType enum에 새로운 메시지를 추가할 수 있습니다:

```java
INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, new String[]{
    "Unexpected internal server error occurred",
    "Custom error message here",  // 추가
    // ...
}),
```

## 참고 자료

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Logback Configuration](https://logback.qos.ch/manual/configuration.html)
