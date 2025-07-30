
# k6 학습 가이드

이 문서는 k6를 사용하여 부하 테스트를 작성하고 실행하는 방법에 대한 기본적인 안내를 제공합니다.

## 1. k6란 무엇인가?

k6는 개발자 중심의 오픈소스 부하 테스트 도구로, Go 언어로 작성되어 성능이 뛰어납니다. 테스트 스크립트는 JavaScript(ES2015/ES6)로 작성하여 실제 브라우저와 유사한 환경에서 API, 마이크로서비스, 웹사이트 등을 테스트할 수 있습니다.

## 2. 주요 특징

- **CLI 기반:** 사용하기 쉬운 명령줄 인터페이스를 제공합니다.
- **JavaScript 스크립트:** 친숙한 JavaScript로 테스트 시나리오를 작성합니다.
- **성능:** Go로 작성되어 적은 리소스로 높은 부하를 생성할 수 있습니다.
- **유연한 설정:** VUs(Virtual Users), 기간(Duration), 단계(Stages) 등 다양한 옵션을 통해 정교한 테스트 시나리오를 구성할 수 있습니다.
- **다양한 프로토콜 지원:** HTTP/1.1, HTTP/2, WebSockets, gRPC 등을 지원합니다.
- **확장성:** 다양한 확장 기능을 통해 필요에 따라 기능을 추가할 수 있습니다.

## 3. 기본 사용법

### 3.1. 테스트 스크립트 작성

k6 테스트는 JavaScript 파일로 작성됩니다. 기본적인 구조는 다음과 같습니다.

```javascript
import http from 'k6/http';
import { sleep } from 'k6';

// 1. 테스트 옵션 설정 (선택 사항)
export const options = {
  vus: 10, // 10명의 가상 사용자
  duration: '30s', // 30초 동안 실행
};

// 2. 테스트 함수 정의
export default function () {
  // 테스트할 API에 GET 요청 보내기
  http.get('https://test.k6.io');

  // 1초 동안 대기
  sleep(1);
}
```

- **`import`**: k6 모듈을 가져옵니다. `http` 모듈은 HTTP 요청을 보내는 데 사용되고, `sleep` 함수는 대기 시간을 설정하는 데 사용됩니다.
- **`options`**: 테스트 실행에 대한 설정을 정의합니다.
  - `vus`: 가상 사용자 수
  - `duration`: 테스트 실행 시간
- **`export default function () {}`**: 각 가상 사용자가 반복적으로 실행할 테스트 로직을 포함하는 메인 함수입니다.

### 3.2. 테스트 실행

터미널에서 다음 명령어를 사용하여 테스트를 실행합니다.

```bash
k6 run <스크립트_파일_이름>.js
```

예시: `k6 run script.js`

### 3.3. 결과 확인

테스트가 완료되면 결과가 터미널에 출력됩니다. 주요 메트릭은 다음과 같습니다.

- **`http_req_duration`**: 요청의 총 소요 시간 (avg, min, med, max, p(90), p(95))
- **`http_reqs`**: 총 HTTP 요청 수
- **`vus`**: 최대 가상 사용자 수
- **`iterations`**: `default` 함수가 실행된 총 횟수

## 4. 반드시 알아야 할 문법

### 4.1. 옵션 (Options)

`options` 객체를 사용하여 테스트 동작을 세밀하게 제어할 수 있습니다.

- **`vus`**: 동시에 실행할 가상 사용자 수
- **`duration`**: 테스트의 총 실행 시간
- **`stages`**: 시간에 따라 VUs를 조절하는 단계별 시나리오를 정의합니다.

```javascript
export const options = {
  stages: [
    { duration: '20s', target: 10 }, // 20초 동안 VUs를 10명까지 서서히 늘림
    { duration: '30s', target: 10 }, // 10명의 VUs를 30초 동안 유지
    { duration: '10s', target: 0 },  // 10초 동안 VUs를 0명으로 서서히 줄임
  ],
};
```

### 4.2. 체크 (Checks)

`check` 함수를 사용하여 응답의 유효성을 검증할 수 있습니다. 테스트의 성공/실패를 판단하는 데 사용됩니다.

```javascript
import { check } from 'k6';
import http from 'k6/http';

export default function () {
  const res = http.get('https://httpbin.org/status/200');

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body size is 0': (r) => r.body.length === 0,
  });
}
```

### 4.3. 임계값 (Thresholds)

`thresholds`를 설정하여 테스트의 성능 목표(SLOs)를 정의하고, 이를 충족하지 못하면 테스트를 실패로 처리할 수 있습니다.

```javascript
export const options = {
  thresholds: {
    // http_req_failed는 1% 미만이어야 함
    'http_req_failed': ['rate<0.01'],

    // http_req_duration의 95%는 300ms 미만이어야 함
    'http_req_duration': ['p(95)<300'],
  },
};
```

### 4.4. 시나리오 (Scenarios)

`scenarios`를 사용하면 여러 테스트 함수를 각기 다른 실행 패턴으로 동시에 실행할 수 있습니다.

```javascript
import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  scenarios: {
    contacts: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      exec: 'contacts', // 실행할 함수 이름
    },
    news: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 100,
      exec: 'news', // 실행할 함수 이름
    },
  },
};

export function contacts() {
  http.get('https://test.k6.io/contacts.php');
  sleep(0.5);
}

export function news() {
  http.get('https://test.k6.io/news.php');
  sleep(1);
}
```

## 5. 추가 학습 자료

- **k6 공식 문서:** [https://k6.io/docs/](https://k6.io/docs/)
- **k6 예제:** [https://k6.io/docs/examples/](https://k6.io/docs/examples/)

이 가이드를 통해 k6의 기본적인 사용법을 익히고, 실제 프로젝트에 적용하여 성능 테스트를 자동화해 보세요.
