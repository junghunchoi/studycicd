#!/bin/sh

# 동적 설정 변경을 위한 스크립트
# 사용법: ./reload-config.sh [LEGACY_WEIGHT] [REFACTORED_WEIGHT] [REFACTORED_PERCENTAGE]

LEGACY_WEIGHT=${1:-${LEGACY_WEIGHT:-95}}
REFACTORED_WEIGHT=${2:-${REFACTORED_WEIGHT:-5}}
REFACTORED_PERCENTAGE=${3:-${REFACTORED_PERCENTAGE:-5}}

echo "Updating NGINX configuration with new weights:"
echo "  Legacy: ${LEGACY_WEIGHT}%"
echo "  Refactored: ${REFACTORED_WEIGHT}%"
echo "  Refactored Percentage: ${REFACTORED_PERCENTAGE}%"

# 환경변수 업데이트
export LEGACY_WEIGHT
export REFACTORED_WEIGHT
export REFACTORED_PERCENTAGE

# 새로운 설정 파일 생성
envsubst '${LEGACY_WEIGHT},${REFACTORED_WEIGHT},${REFACTORED_PERCENTAGE}' \
    < /etc/nginx/conf.d/default.conf.template \
    > /etc/nginx/conf.d/default.conf

# 설정 검증
nginx -t

if [ $? -eq 0 ]; then
    echo "New configuration is valid, reloading NGINX..."
    nginx -s reload
    echo "NGINX reloaded successfully"
    
    # 설정 변경 로그
    echo "$(date): Weight changed to Legacy:${LEGACY_WEIGHT}%, Refactored:${REFACTORED_WEIGHT}%, Percentage:${REFACTORED_PERCENTAGE}%" >> /var/log/nginx/weight-changes.log
else
    echo "New configuration is invalid, keeping current configuration"
    exit 1
fi