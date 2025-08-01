#!/bin/bash

# 실무용 웹훅 기반 자동 롤백 스크립트
# Alertmanager에서 호출되는 자동 롤백 처리

set -euo pipefail

# 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="/var/log/nginx/rollback.log"
LOCK_FILE="/tmp/rollback.lock"
ROLLBACK_TIMEOUT=30
NGINX_CONTAINER="nginx-lb"
ROLLBACK_SCRIPT="/reload-config.sh"

# 로깅 함수
log() {
    local level="$1"
    shift
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*" | tee -a "$LOG_FILE"
}

# 잠금 파일 확인 (동시 실행 방지)
acquire_lock() {
    if [ -f "$LOCK_FILE" ]; then
        local lock_pid=$(cat "$LOCK_FILE" 2>/dev/null || echo "")
        if [ -n "$lock_pid" ] && kill -0 "$lock_pid" 2>/dev/null; then
            log "WARN" "Rollback already in progress (PID: $lock_pid)"
            exit 1
        else
            log "INFO" "Removing stale lock file"
            rm -f "$LOCK_FILE"
        fi
    fi
    
    echo $$ > "$LOCK_FILE"
    log "INFO" "Acquired rollback lock (PID: $$)"
}

# 잠금 해제
release_lock() {
    rm -f "$LOCK_FILE"
    log "INFO" "Released rollback lock"
}

# 트랩으로 정리 작업 보장
trap 'release_lock; exit' EXIT INT TERM

# Alertmanager 웹훅 페이로드 파싱
parse_webhook_payload() {
    local payload="$1"
    
    # jq로 알람 정보 추출
    if command -v jq >/dev/null 2>&1; then
        ALERT_NAME=$(echo "$payload" | jq -r '.alerts[0].labels.alertname // "Unknown"')
        ERROR_RATE=$(echo "$payload" | jq -r '.alerts[0].annotations.description // "Unknown"' | grep -oE '[0-9]+\.[0-9]+%' | head -1 || echo "Unknown")
        SERVICE=$(echo "$payload" | jq -r '.alerts[0].labels.service // "Unknown"')
        ROLLBACK_REASON=$(echo "$payload" | jq -r '.alerts[0].annotations.rollback_reason // "High error rate"')
        
        log "INFO" "Parsed webhook - Alert: $ALERT_NAME, Service: $SERVICE, Error Rate: $ERROR_RATE"
    else
        log "WARN" "jq not available, using default values"
        ALERT_NAME="CanaryAutoRollback"
        ERROR_RATE="Unknown"
        SERVICE="canary-deployment"
        ROLLBACK_REASON="High error rate"
    fi
}

# 현재 트래픽 가중치 확인
get_current_weights() {
    local legacy_weight
    local refactored_weight
    
    # Docker exec로 현재 설정 확인
    if command -v docker >/dev/null 2>&1; then
        # NGINX 설정에서 현재 가중치 추출 (간단한 방법)
        legacy_weight=$(docker exec "$NGINX_CONTAINER" grep -E "server.*weight=" /etc/nginx/conf.d/default.conf | grep legacy | grep -oE 'weight=[0-9]+' | head -1 | cut -d= -f2 || echo "95")
        refactored_weight=$(docker exec "$NGINX_CONTAINER" grep -E "server.*weight=" /etc/nginx/conf.d/default.conf | grep refactored | grep -oE 'weight=[0-9]+' | head -1 | cut -d= -f2 || echo "5")
    else
        legacy_weight="95"
        refactored_weight="5"
    fi
    
    log "INFO" "Current weights - Legacy: ${legacy_weight}%, Refactored: ${refactored_weight}%"
    echo "$legacy_weight $refactored_weight"
}

# 자동 롤백 실행
execute_rollback() {
    local start_time=$(date +%s)
    
    log "CRITICAL" "🚨 EXECUTING AUTOMATIC ROLLBACK 🚨"
    log "CRITICAL" "Alert: $ALERT_NAME"
    log "CRITICAL" "Service: $SERVICE"
    log "CRITICAL" "Reason: $ROLLBACK_REASON"
    log "CRITICAL" "Error Rate: $ERROR_RATE"
    
    # 현재 가중치 확인
    local weights
    weights=$(get_current_weights)
    local current_legacy=$(echo "$weights" | cut -d' ' -f1)
    local current_refactored=$(echo "$weights" | cut -d' ' -f2)
    
    # 이미 완전 롤백 상태인지 확인
    if [ "$current_legacy" = "100" ] && [ "$current_refactored" = "0" ]; then
        log "INFO" "Already in full rollback state (Legacy: 100%, Refactored: 0%)"
        return 0
    fi
    
    # Docker exec로 롤백 실행
    log "INFO" "Executing rollback command..."
    if timeout "$ROLLBACK_TIMEOUT" docker exec "$NGINX_CONTAINER" "$ROLLBACK_SCRIPT" 100 0 0; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        log "CRITICAL" "✅ ROLLBACK COMPLETED SUCCESSFULLY"
        log "CRITICAL" "Duration: ${duration}s"
        log "CRITICAL" "Traffic: Legacy 100%, Refactored 0%"
        
        # 성공 알림 (선택적)
        send_success_notification
        
        return 0
    else
        log "ERROR" "❌ ROLLBACK FAILED"
        log "ERROR" "Command timed out or failed after ${ROLLBACK_TIMEOUT}s"
        
        # 실패 알림
        send_failure_notification
        
        return 1
    fi
}

# 성공 알림 (Slack, Email 등)
send_success_notification() {
    log "INFO" "Sending rollback success notification"
    
    # Slack 웹훅 예시 (실제 환경에서 사용)
    local slack_webhook="${SLACK_WEBHOOK_URL:-}"
    if [ -n "$slack_webhook" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"✅ **자동 롤백 성공**\nService: $SERVICE\nAlert: $ALERT_NAME\nReason: $ROLLBACK_REASON\nTime: $(date)\"}" \
            "$slack_webhook" 2>/dev/null || log "WARN" "Failed to send Slack notification"
    fi
}

# 실패 알림
send_failure_notification() {
    log "ERROR" "Sending rollback failure notification"
    
    local slack_webhook="${SLACK_WEBHOOK_URL:-}"
    if [ -n "$slack_webhook" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"❌ **자동 롤백 실패**\nService: $SERVICE\nAlert: $ALERT_NAME\n긴급 수동 개입 필요!\nTime: $(date)\"}" \
            "$slack_webhook" 2>/dev/null || log "WARN" "Failed to send Slack notification"
    fi
}

# 메인 실행 함수
main() {
    local webhook_payload="${1:-}"
    
    log "INFO" "=== Webhook Rollback Started ==="
    log "INFO" "Script: $0"
    log "INFO" "PID: $$"
    log "INFO" "User: $(whoami)"
    
    # 잠금 획득
    acquire_lock
    
    # 웹훅 페이로드 파싱
    if [ -n "$webhook_payload" ]; then
        parse_webhook_payload "$webhook_payload"
    else
        log "WARN" "No webhook payload provided, using default values"
        ALERT_NAME="Manual"
        ERROR_RATE="Manual trigger"
        SERVICE="canary-deployment"
        ROLLBACK_REASON="Manual rollback"
    fi
    
    # 롤백 실행
    if execute_rollback; then
        log "INFO" "=== Webhook Rollback Completed Successfully ==="
        exit 0
    else
        log "ERROR" "=== Webhook Rollback Failed ==="
        exit 1
    fi
}

# HTTP 서버 모드 (실무에서 사용)
if [ "${1:-}" = "--server" ]; then
    log "INFO" "Starting webhook server mode on port 8090"
    
    # 간단한 HTTP 서버로 웹훅 수신
    while true; do
        echo -e "HTTP/1.1 200 OK\nContent-Length: 2\n\nOK" | nc -l -p 8090 -q 1 > /tmp/webhook_payload.json &
        wait
        
        if [ -s /tmp/webhook_payload.json ]; then
            log "INFO" "Received webhook request"
            main "$(cat /tmp/webhook_payload.json)" &
        fi
        
        rm -f /tmp/webhook_payload.json
        sleep 1
    done
else
    # 직접 실행 모드
    main "$@"
fi