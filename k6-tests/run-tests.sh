#!/bin/bash

# k6 테스트 실행 스크립트

set -e

# 기본 설정
BASE_URL=${BASE_URL:-"http://localhost"}
CONTROLLER_URL=${CONTROLLER_URL:-"http://localhost:8080"}

echo "=== K6 테스트 실행 스크립트 ==="
echo "Base URL: $BASE_URL"
echo "Controller URL: $CONTROLLER_URL"
echo ""

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo "Error: k6가 설치되어 있지 않습니다."
    echo "설치 방법:"
    echo "  macOS: brew install k6"
    echo "  Linux: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# 서비스 상태 확인
echo "서비스 상태 확인 중..."
if ! curl -s -f "$BASE_URL/health" > /dev/null; then
    echo "Warning: 애플리케이션($BASE_URL)에 연결할 수 없습니다."
    echo "docker-compose up -d 명령으로 서비스를 시작하세요."
fi

if ! curl -s -f "$CONTROLLER_URL/api/traffic/status" > /dev/null; then
    echo "Warning: 컨트롤러($CONTROLLER_URL)에 연결할 수 없습니다."
    echo "컨트롤러 서비스가 실행 중인지 확인하세요."
fi

echo ""

# 테스트 메뉴
while true; do
    echo "실행할 테스트를 선택하세요:"
    echo "1) 트래픽 컨트롤 API 테스트"
    echo "2) 카나리 배포 시뮬레이션"
    echo "3) 부하 테스트"
    echo "4) 모든 테스트 실행"
    echo "5) 종료"
    echo ""
    read -p "선택 (1-5): " choice

    case $choice in
        1)
            echo ""
            echo "=== 트래픽 컨트롤 API 테스트 실행 ==="
            k6 run \
                --env BASE_URL="$BASE_URL" \
                --env CONTROLLER_URL="$CONTROLLER_URL" \
                traffic-control-test.js
            ;;
        2)
            echo ""
            echo "=== 카나리 배포 시뮬레이션 실행 ==="
            echo "이 테스트는 약 10분 소요됩니다."
            read -p "계속하시겠습니까? (y/N): " confirm
            if [[ $confirm =~ ^[Yy]$ ]]; then
                k6 run \
                    --env BASE_URL="$BASE_URL" \
                    --env CONTROLLER_URL="$CONTROLLER_URL" \
                    canary-deployment-test.js
            fi
            ;;
        3)
            echo ""
            echo "=== 부하 테스트 실행 ==="
            echo "이 테스트는 약 15분 소요됩니다."
            read -p "계속하시겠습니까? (y/N): " confirm
            if [[ $confirm =~ ^[Yy]$ ]]; then
                k6 run \
                    --env BASE_URL="$BASE_URL" \
                    --env CONTROLLER_URL="$CONTROLLER_URL" \
                    load-test.js
            fi
            ;;
        4)
            echo ""
            echo "=== 모든 테스트 실행 ==="
            echo "전체 테스트는 약 30분 소요됩니다."
            read -p "계속하시겠습니까? (y/N): " confirm
            if [[ $confirm =~ ^[Yy]$ ]]; then
                echo "1. 트래픽 컨트롤 API 테스트..."
                k6 run \
                    --env BASE_URL="$BASE_URL" \
                    --env CONTROLLER_URL="$CONTROLLER_URL" \
                    traffic-control-test.js
                
                echo ""
                echo "2. 부하 테스트..."
                k6 run \
                    --env BASE_URL="$BASE_URL" \
                    --env CONTROLLER_URL="$CONTROLLER_URL" \
                    load-test.js
                
                echo ""
                echo "3. 카나리 배포 시뮬레이션..."
                k6 run \
                    --env BASE_URL="$BASE_URL" \
                    --env CONTROLLER_URL="$CONTROLLER_URL" \
                    canary-deployment-test.js
            fi
            ;;
        5)
            echo "테스트를 종료합니다."
            exit 0
            ;;
        *)
            echo "잘못된 선택입니다. 1-5 중에서 선택하세요."
            ;;
    esac
    
    echo ""
    echo "테스트가 완료되었습니다."
    echo ""
done