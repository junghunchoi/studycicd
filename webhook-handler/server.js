#!/usr/bin/env node

/**
 * 실무용 웹훅 핸들러 서버
 * Alertmanager에서 전송되는 웹훅을 받아 자동 롤백을 처리
 */

const express = require('express');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 8090;
const LOG_FILE = '/var/log/webhook/webhook-handler.log';

// 미들웨어 설정
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// 로깅 함수
function log(level, message, data = null) {
    const timestamp = new Date().toISOString();
    const logMessage = `${timestamp} [${level}] ${message}`;
    
    console.log(logMessage);
    
    // 파일 로깅
    try {
        const logDir = path.dirname(LOG_FILE);
        if (!fs.existsSync(logDir)) {
            fs.mkdirSync(logDir, { recursive: true });
        }
        
        const fullMessage = data 
            ? `${logMessage}\nData: ${JSON.stringify(data, null, 2)}\n`
            : `${logMessage}\n`;
            
        fs.appendFileSync(LOG_FILE, fullMessage);
    } catch (error) {
        console.error('Failed to write log:', error.message);
    }
}

// 롤백 스크립트 실행 함수
function executeRollback(webhookData, callback) {
    const scriptPath = '/scripts/webhook-rollback.sh';
    const payload = JSON.stringify(webhookData);
    
    log('INFO', 'Executing rollback script', { scriptPath, payload: webhookData });
    
    const rollbackProcess = spawn('bash', [scriptPath], {
        stdio: ['pipe', 'pipe', 'pipe'],
        env: {
            ...process.env,
            SLACK_WEBHOOK_URL: process.env.SLACK_WEBHOOK_URL || ''
        }
    });
    
    // 페이로드를 stdin으로 전달
    rollbackProcess.stdin.write(payload);
    rollbackProcess.stdin.end();
    
    let stdout = '';
    let stderr = '';
    
    rollbackProcess.stdout.on('data', (data) => {
        stdout += data.toString();
    });
    
    rollbackProcess.stderr.on('data', (data) => {
        stderr += data.toString();
    });
    
    // 30초 타임아웃
    const timeout = setTimeout(() => {
        rollbackProcess.kill('SIGKILL');
        callback(new Error('Rollback script timeout'), null);
    }, 30000);
    
    rollbackProcess.on('close', (code) => {
        clearTimeout(timeout);
        
        if (code === 0) {
            log('INFO', 'Rollback script completed successfully', { stdout, stderr });
            callback(null, { success: true, stdout, stderr });
        } else {
            log('ERROR', 'Rollback script failed', { code, stdout, stderr });
            callback(new Error(`Rollback failed with code ${code}`), { success: false, code, stdout, stderr });
        }
    });
    
    rollbackProcess.on('error', (error) => {
        clearTimeout(timeout);
        log('ERROR', 'Failed to execute rollback script', error);
        callback(error, null);
    });
}

// 알람 검증 함수
function validateAlert(webhookData) {
    if (!webhookData || !webhookData.alerts || !Array.isArray(webhookData.alerts)) {
        return { valid: false, error: 'Invalid webhook format: missing alerts array' };
    }
    
    if (webhookData.alerts.length === 0) {
        return { valid: false, error: 'No alerts in webhook data' };
    }
    
    const alert = webhookData.alerts[0];
    if (!alert.labels || !alert.annotations) {
        return { valid: false, error: 'Alert missing required labels or annotations' };
    }
    
    return { valid: true };
}

// 자동 롤백 웹훅 엔드포인트 (가장 중요)
app.post('/rollback', (req, res) => {
    const webhookData = req.body;
    const clientIP = req.ip || req.connection.remoteAddress;
    
    log('CRITICAL', '🚨 Auto-rollback webhook received', { 
        source: clientIP, 
        alertCount: webhookData?.alerts?.length || 0,
        groupLabels: webhookData?.groupLabels
    });
    
    // 웹훅 데이터 검증
    const validation = validateAlert(webhookData);
    if (!validation.valid) {
        log('ERROR', 'Invalid webhook data', validation);
        return res.status(400).json({
            success: false,
            error: validation.error
        });
    }
    
    // 즉시 응답 (Alertmanager 타임아웃 방지)
    res.status(200).json({
        success: true,
        message: 'Rollback initiated',
        timestamp: new Date().toISOString()
    });
    
    // 비동기로 롤백 실행
    executeRollback(webhookData, (error, result) => {
        if (error) {
            log('CRITICAL', '❌ Rollback execution failed', error);
            
            // 실패 시 긴급 알림 (실제 환경에서는 PagerDuty, SMS 등)
            if (process.env.EMERGENCY_WEBHOOK_URL) {
                const emergencyData = {
                    text: `🆘 ROLLBACK FAILED - MANUAL INTERVENTION REQUIRED\nError: ${error.message}\nTime: ${new Date().toISOString()}`
                };
                
                // 긴급 웹훅 호출 (논블로킹)
                fetch(process.env.EMERGENCY_WEBHOOK_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(emergencyData)
                }).catch(err => log('ERROR', 'Emergency webhook failed', err));
            }
        } else {
            log('CRITICAL', '✅ Rollback execution successful', result);
        }
    });
});

// 크리티컬 알람 웹훅
app.post('/critical', (req, res) => {
    const webhookData = req.body;
    
    log('WARNING', 'Critical alert webhook received', {
        alertCount: webhookData?.alerts?.length || 0,
        groupLabels: webhookData?.groupLabels
    });
    
    res.status(200).json({
        success: true,
        message: 'Critical alert processed',
        timestamp: new Date().toISOString()
    });
    
    // 크리티컬 알람 처리 (팀 통보, 로깅 등)
    if (webhookData?.alerts) {
        webhookData.alerts.forEach(alert => {
            log('CRITICAL', 'Critical Alert', {
                name: alert.labels?.alertname,
                service: alert.labels?.service,
                summary: alert.annotations?.summary,
                description: alert.annotations?.description
            });
        });
    }
});

// 일반 알람 웹훅
app.post('/general', (req, res) => {
    const webhookData = req.body;
    
    log('INFO', 'General alert webhook received', {
        alertCount: webhookData?.alerts?.length || 0
    });
    
    res.status(200).json({
        success: true,
        message: 'General alert processed',
        timestamp: new Date().toISOString()
    });
});

// 기본 웹훅
app.post('/default', (req, res) => {
    log('INFO', 'Default webhook received');
    res.status(200).json({
        success: true,
        message: 'Default webhook processed'
    });
});

// 헬스 체크
app.get('/health', (req, res) => {
    res.status(200).json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

// 상태 정보
app.get('/status', (req, res) => {
    res.status(200).json({
        service: 'webhook-handler',
        version: '1.0.0',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
        memory: process.memoryUsage(),
        environment: {
            nodeVersion: process.version,
            platform: process.platform,
            arch: process.arch
        }
    });
});

// 에러 핸들링
app.use((error, req, res, next) => {
    log('ERROR', 'Express error handler', error);
    res.status(500).json({
        success: false,
        error: 'Internal server error',
        timestamp: new Date().toISOString()
    });
});

// 404 핸들링
app.use('*', (req, res) => {
    log('WARN', `404 Not Found: ${req.method} ${req.originalUrl}`);
    res.status(404).json({
        success: false,
        error: 'Endpoint not found',
        method: req.method,
        path: req.originalUrl
    });
});

// 서버 시작
const server = app.listen(PORT, '0.0.0.0', () => {
    log('INFO', `🚀 Webhook handler server started on port ${PORT}`);
    log('INFO', `Environment: ${process.env.NODE_ENV || 'development'}`);
    log('INFO', `Log file: ${LOG_FILE}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    log('INFO', 'SIGTERM received, shutting down gracefully');
    server.close(() => {
        log('INFO', 'Server closed');
        process.exit(0);
    });
});

process.on('SIGINT', () => {
    log('INFO', 'SIGINT received, shutting down gracefully');
    server.close(() => {
        log('INFO', 'Server closed');
        process.exit(0);
    });
});

// 처리되지 않은 예외 처리
process.on('uncaughtException', (error) => {
    log('CRITICAL', 'Uncaught Exception', error);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    log('CRITICAL', 'Unhandled Rejection', { reason, promise });
});

module.exports = app;