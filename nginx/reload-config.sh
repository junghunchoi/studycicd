#!/bin/sh

# 동적 설정 변경을 위한 스크립트
# 사용법: ./reload-config.sh [LEGACY_WEIGHT] [REFACTORED_WEIGHT] [REFACTORED_PERCENTAGE]

# 파라미터 설정 (우선순위: 명령행 인자 > 환경변수 > 기본값)
# ${1:-${LEGACY_WEIGHT:-95}} 의미:
# - $1이 있으면 $1 사용
# - $1이 없으면 환경변수 LEGACY_WEIGHT 사용  
# - 환경변수도 없으면 기본값 95 사용
LEGACY_WEIGHT=${1:-${LEGACY_WEIGHT:-95}}
REFACTORED_WEIGHT=${2:-${REFACTORED_WEIGHT:-5}}
REFACTORED_PERCENTAGE=${3:-${REFACTORED_PERCENTAGE:-5}}

# 변경될 가중치 정보 출력
echo "Updating NGINX configuration with new weights:"
echo "  Legacy: ${LEGACY_WEIGHT}%"
echo "  Refactored: ${REFACTORED_WEIGHT}%"
echo "  Refactored Percentage: ${REFACTORED_PERCENTAGE}%"

# 환경변수로 export하여 envsubst가 사용할 수 있도록 설정
export LEGACY_WEIGHT
export REFACTORED_WEIGHT
export REFACTORED_PERCENTAGE

# envsubst: 템플릿 파일의 환경변수 placeholder를 실제 값으로 치환
# '${LEGACY_WEIGHT},${REFACTORED_WEIGHT},${REFACTORED_PERCENTAGE}': 치환할 변수 목록 지정
# < 템플릿파일 > 실제설정파일: 입력을 템플릿에서 받아 출력을 실제 설정파일로 리다이렉트
envsubst '${LEGACY_WEIGHT},${REFACTORED_WEIGHT},${REFACTORED_PERCENTAGE}' \
    < /etc/nginx/conf.d/default.conf.template \
    > /etc/nginx/conf.d/default.conf

# nginx -t: 새로 생성된 설정파일의 문법 검증
nginx -t

# $?: 직전 명령의 종료 코드 (0=성공, 0이 아님=실패)
if [ $? -eq 0 ]; then
    echo "New configuration is valid, reloading NGINX..."
    # nginx -s reload: 무중단으로 설정 다시 로드 (graceful reload)
    nginx -s reload
    echo "NGINX reloaded successfully"
    
    # 설정 변경 이력을 로그 파일에 기록
    # $(date): 현재 날짜/시간을 명령 치환으로 삽입
    # >>: 파일 끝에 추가 (기존 내용 유지)
    echo "$(date): Weight changed to Legacy:${LEGACY_WEIGHT}%, Refactored:${REFACTORED_WEIGHT}%, Percentage:${REFACTORED_PERCENTAGE}%" >> /var/log/nginx/weight-changes.log
else
    # 설정이 잘못된 경우 변경하지 않고 종료
    echo "New configuration is invalid, keeping current configuration"
    exit 1  # 스크립트를 에러 코드 1로 종료
fi