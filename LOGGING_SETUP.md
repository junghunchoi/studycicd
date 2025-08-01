# 카나리 배포 로깅 설정 가이드

## 개요
이 설정은 NGINX와 Spring Boot 애플리케이션의 요청 흐름을 추적하고 Loki를 통해 중앙집중식 로그 관리를 제공합니다.

## 설정된 구성 요소

### 1. NGINX 로깅
- **canary.log**: 카나리 배포 추적용 상세 로그
- **routing.log**: 업스트림 라우팅 정보
- **access.log**: 표준 접근 로그
- **error.log**: 에러 로그

### 2. Spring Boot 애플리케이션 로깅
- **legacy-application.log**: Legacy 버전 애플리케이션 로그
- **refactored-application.log**: Refactored 버전 애플리케이션 로그
- **access_log.yyyy-MM-dd**: Tomcat 접근 로그

### 3. 로그 수집 및 시각화
- **Loki**: 로그 집계 및 저장
- **Promtail**: 로그 수집 에이전트
- **Grafana**: 로그 시각화 및 대시보드

## 실행 방법

1. **시스템 시작**:
   ```bash
   docker-compose up -d
   ```

2. **로그 확인**:
   ```bash
   # NGINX 로그
   docker exec nginx-lb tail -f /var/log/nginx/canary.log
   
   # 애플리케이션 로그
   docker exec legacy-app-1 tail -f /var/log/app/legacy-application.log
   docker exec refactored-app-1 tail -f /var/log/app/refactored-application.log
   ```

3. **서비스 접속**:
   - NGINX 로드밸런서: http://localhost:8000
   - Grafana: http://localhost:3000 (admin/admin)
   - Prometheus: http://localhost:9090
   - Loki: http://localhost:3100
   - 대시보드: "Canary Deployment Logs"

## 로그 포맷

### NGINX Canary Log
```
2024-01-15T10:30:45+00:00|NGINX|req-123|192.168.1.1|"GET /api/test"|200|1024|0.125|legacy-app-1:8080|0.120|0.005|"curl/7.68.0"|""|MISS
```

### Spring Boot Application Log
```
2024-01-15 10:30:45.123|LEGACY|http-nio-8080-exec-1|RequestLoggingInterceptor|req-123|legacy-app-1:8080|REQUEST_START|GET|/api/test
```

### Tomcat Access Log
```
[15/Jan/2024:10:30:45 +0000]|192.168.1.1|GET /api/test HTTP/1.1|200|1024|125|req-123|5
```

## 주요 추적 정보

1. **Request ID**: 모든 로그에서 요청을 추적할 수 있는 고유 식별자
2. **Upstream Address**: 어느 백엔드 서버로 요청이 라우팅되었는지 확인
3. **Response Time**: NGINX와 애플리케이션의 응답 시간
4. **Version Tracking**: Legacy vs Refactored 버전 구분

## Grafana 대시보드 기능

1. **NGINX Request Flow**: 모든 NGINX 요청의 실시간 흐름
2. **Legacy App Requests**: Legacy 애플리케이션 요청 로그
3. **Refactored App Requests**: Refactored 애플리케이션 요청 로그
4. **Request Distribution**: 업스트림별 요청 분배 현황
5. **Response Times**: 버전별 응답 시간 통계
6. **Error Logs**: 모든 에러 로그 통합 뷰
7. **Request Tracing**: Request ID로 전체 요청 경로 추적

## 로그 쿼리 예제

### Loki 쿼리
```
# 특정 Request ID 추적
{job=~"nginx|spring-boot"} |~ "req-123"

# Legacy 앱 에러 로그
{job="spring-boot", service="legacy-app"} |= "ERROR"

# 느린 요청 찾기
{job="nginx", log_type="canary"} | regex "request_time=(?P<time>[0-9.]+)" | time > 1.0

# 특정 업스트림 요청
{job="nginx", log_type="routing"} |~ "upstream=legacy-app-1"
```

## 문제 해결

1. **로그가 보이지 않는 경우**:
   - 볼륨 마운트 확인: `docker volume ls`
   - Promtail 로그 확인: `docker logs promtail`
   - Loki 연결 확인: `curl http://localhost:3100/ready`

2. **Grafana에서 데이터가 없는 경우**:
   - Loki 데이터소스 연결 테스트
   - 시간 범위 확인 (최근 30분)
   - 로그 레이블 확인

3. **애플리케이션 로그 디렉토리 권한**:
   ```bash
   # 컨테이너 내부에서 로그 디렉토리 생성 확인
   docker exec legacy-app-1 ls -la /var/log/app/
   ```

## 추가 설정

### 로그 보관 정책
- Loki: 7일간 보관 (설정에서 변경 가능)
- 로그 파일: 30일 롤링, 최대 1GB

### 성능 최적화
- 로그 수집 빈도: 5초마다
- 최대 라인 수: 1000줄
- 압축 활성화