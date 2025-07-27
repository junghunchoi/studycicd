#!/bin/sh

# 환경변수 기본값 설정
export LEGACY_WEIGHT=${LEGACY_WEIGHT:-95}
export REFACTORED_WEIGHT=${REFACTORED_WEIGHT:-5}
export REFACTORED_PERCENTAGE=${REFACTORED_PERCENTAGE:-5}

echo "Starting NGINX with traffic weights:"
echo "  Legacy: ${LEGACY_WEIGHT}%"
echo "  Refactored: ${REFACTORED_WEIGHT}%"
echo "  Refactored Percentage: ${REFACTORED_PERCENTAGE}%"

# 템플릿에서 실제 설정 파일 생성
envsubst '${LEGACY_WEIGHT},${REFACTORED_WEIGHT},${REFACTORED_PERCENTAGE}' \
    < /etc/nginx/conf.d/default.conf.template \
    > /etc/nginx/conf.d/default.conf

# nginx 설정 검증
nginx -t

if [ $? -eq 0 ]; then
    echo "NGINX configuration is valid"
    # nginx 시작
    exec nginx -g "daemon off;"
else
    echo "NGINX configuration is invalid"
    exit 1
fi