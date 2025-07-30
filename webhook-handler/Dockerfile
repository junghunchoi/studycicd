FROM node:18-alpine

# 실무용 보안 설정
RUN addgroup -g 1001 -S nodejs && \
    adduser -S webhook -u 1001

# 필요한 시스템 패키지 설치
RUN apk add --no-cache \
    bash \
    curl \
    jq \
    wget \
    docker-cli \
    && rm -rf /var/cache/apk/*

# 작업 디렉토리 설정
WORKDIR /app

# package.json과 package-lock.json 복사 (캐싱 최적화)
COPY package*.json ./

# 의존성 설치
RUN npm ci --only=production && \
    npm cache clean --force

# 애플리케이션 코드 복사
COPY server.js ./

# 로그 디렉토리 생성
RUN mkdir -p /var/log/webhook && \
    chown -R webhook:nodejs /var/log/webhook

# 비루트 사용자로 실행
USER webhook

# 포트 노출
EXPOSE 8090

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8090/health || exit 1

# 애플리케이션 실행
CMD ["node", "server.js"]