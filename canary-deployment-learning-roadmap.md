# 카나리 배포 학습 로드맵

## 📋 개요

이 로드맵은 현재 프로젝트(`studycicd`)를 통해 카나리 배포를 체계적으로 학습하기 위한 가이드입니다. NGINX 로드밸런싱, 모니터링, 자동 롤백 시스템을 단계별로 이해하고 실습할 수 있도록 구성되었습니다.

## 🏗️ 현재 프로젝트 분석

### 인프라 구성
- **애플리케이션**: Spring Boot 3.5.4 (Java 21)
- **컨테이너화**: Docker Compose 기반
- **로드밸런서**: NGINX (가중치 기반 라우팅)
- **모니터링**: Prometheus + Grafana + Alertmanager
- **테스트**: K6 부하 테스트 스크립트

### 서비스 구조
```
NGINX (포트 80) → Legacy Apps (8080, 8081) + Refactored Apps (9080, 9081)
                ↓
            Prometheus (9090) → Grafana (3000) + Alertmanager (9093)
```

---

## 🎯 학습 단계

### 1단계: NGINX 로드밸런싱 및 카나리 트래픽 처리

#### 🔴 필수 학습 내용

1. **NGINX 기본 개념**
   - 리버스 프록시와 로드밸런서 역할의 차이
   - upstream 블록과 가중치 기반 라우팅
   - 헬스체크 메커니즘 (`proxy_next_upstream`)

2. **동적 설정 변경**
   - 템플릿 기반 설정 관리 (`default.conf.template`)
   - 환경변수를 통한 런타임 가중치 조정
   - `nginx -s reload`를 통한 무중단 설정 적용

3. **실습 과제**
   - `docker-compose up -d`로 전체 시스템 시작
   - `nginx/conf.d/default.conf.template` 파일 분석
   - 환경변수 `LEGACY_WEIGHT`, `REFACTORED_WEIGHT` 값 변경 테스트
   - `http://localhost`로 요청 보내서 트래픽 분산 확인

#### 🟡 추가 학습 내용

1. **고급 라우팅 전략**
   - 헤더 기반 라우팅 (`$http_canary_user`)
   - 지리적 위치 기반 라우팅
   - Sticky Session 구현

2. **성능 최적화**
   - Connection pooling 설정
   - 압축 및 캐싱 전략
   - Rate limiting 구현

---

### 2단계: 모니터링 설정 및 메트릭 수집

#### 🔴 필수 학습 내용

1. **Prometheus 메트릭 수집**
   - Spring Boot Actuator `/actuator/prometheus` 엔드포인트
   - NGINX Exporter 메트릭 (`nginx-exporter:9113`)
   - 커스텀 메트릭 정의 및 수집

2. **Grafana 대시보드**
   - 실시간 트래픽 분산 현황 시각화
   - 버전별 에러율 및 응답시간 비교
   - 알림 규칙 설정

3. **실습 과제**
   - Grafana 접속 (`http://localhost:3000`, admin/admin)
   - 기본 대시보드 분석 및 커스터마이징
   - Prometheus 쿼리 작성 (`http://localhost:9090`)
   - 애플리케이션별 메트릭 확인

#### 🟡 추가 학습 내용

1. **고급 메트릭 분석**
   - SLI/SLO 정의 및 측정
   - 분위수 기반 성능 분석
   - 비즈니스 메트릭 트래킹

2. **알림 최적화**
   - 알림 피로도 방지 전략
   - 에스컬레이션 정책
   - 알림 채널 다양화 (Slack, Email, Webhook)

---

### 3단계: 자동 롤백 전략 및 구현

#### 🔴 필수 학습 내용

1. **Alertmanager 설정**
   - `monitoring/alertmanager/alertmanager.yml` 분석
   - 임계값 기반 알림 규칙 (`canary-alerts.yml`)
   - 웹훅을 통한 자동화 트리거

2. **Controller 서비스**
   - `src/main/java/junghun/studycicd/controller/TrafficController.java` 분석
   - REST API를 통한 트래픽 가중치 조정
   - 배포 상태 관리 및 롤백 로직

3. **실습 과제**
   - K6 테스트 실행: `./k6-tests/run-tests.sh`
   - 의도적인 에러 발생으로 롤백 테스트
   - Controller API 직접 호출 테스트
   - Alertmanager 알림 확인 (`http://localhost:9093`)

#### 🟡 추가 학습 내용

1. **고급 롤백 전략**
   - 부분 롤백 (특정 사용자 그룹만)
   - 단계적 롤백 (점진적 트래픽 감소)
   - Circuit breaker 패턴 적용

2. **안전장치 구현**
   - 동시 배포 방지
   - 최대 롤백 횟수 제한
   - 긴급 정지 기능

---

## 🛠️ 실습 시나리오

### 시나리오 1: 기본 카나리 배포
1. 전체 시스템 시작: `docker-compose up -d`
2. 초기 트래픽 확인 (Legacy 95%, Refactored 5%)
3. Grafana에서 트래픽 분산 모니터링
4. 점진적 트래픽 증가: 5% → 10% → 25% → 50% → 100%

### 시나리오 2: 자동 롤백 테스트
1. Refactored 앱에 의도적 에러 주입
2. 트래픽을 Refactored로 증가
3. 임계값 초과로 인한 자동 알림 발생
4. Alertmanager → Controller → NGINX 롤백 프로세스 확인

### 시나리오 3: 부하 테스트
1. K6 부하 테스트 실행
2. 부하 상황에서의 카나리 배포 안정성 검증
3. 성능 임계값 모니터링
4. 자원 사용량 분석

---

## 📚 추가 학습 리소스

### 핵심 파일 분석 순서
1. `docker-compose.yml` - 전체 인프라 구조
2. `nginx/conf.d/default.conf.template` - 로드밸런싱 설정
3. `monitoring/prometheus/prometheus.yml` - 메트릭 수집 설정
4. `monitoring/prometheus/rules/canary-alerts.yml` - 알림 규칙
5. `src/main/java/junghun/studycicd/controller/TrafficController.java` - 트래픽 제어

### 개념 이해를 위한 학습 자료
- **NGINX 공식 문서**: upstream 모듈, 헬스체크
- **Prometheus 쿼리 언어**: PromQL 기본 문법
- **Spring Boot Actuator**: 메트릭 엔드포인트
- **Docker Compose**: 네트워킹, 볼륨, 헬스체크

---

## ✅ 학습 완료 체크리스트

### 1단계 완료 기준
- [ ] NGINX 가중치 변경으로 트래픽 분산 조정 가능
- [ ] 무중단으로 설정 변경 및 적용 가능
- [ ] 헬스체크 실패 시 자동 백엔드 제외 확인

### 2단계 완료 기준
- [ ] Grafana에서 실시간 메트릭 모니터링 가능
- [ ] 커스텀 대시보드 생성 및 수정 가능
- [ ] Prometheus 쿼리로 원하는 메트릭 추출 가능

### 3단계 완료 기준
- [ ] 자동 롤백 시나리오 성공적으로 실행
- [ ] Controller API로 수동 트래픽 조정 가능
- [ ] 알림 규칙 수정 및 테스트 완료

---

## 🎯 최종 목표

이 학습 과정을 통해 다음 역량을 갖추게 됩니다:
- **카나리 배포 설계 및 구현 능력**
- **모니터링 기반 의사결정 역량**
- **자동화된 롤백 시스템 구축 경험**
- **프로덕션 환경 운영 노하우**

각 단계를 차근차근 따라가며 이론과 실습을 병행하면, 실제 프로덕션 환경에서도 안전한 카나리 배포를 구현할 수 있는 전문성을 키울 수 있습니다.