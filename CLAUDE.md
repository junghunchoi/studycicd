# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a comprehensive Spring Boot 3.5.4 application using Java 21 and Gradle, designed for learning and practicing real-world canary deployment strategies. The project features a complete CI/CD ecosystem with Docker Compose, NGINX load balancing, monitoring stack, and automated deployment systems.

## Common Commands

### Build and Test
- `./gradlew build` - Build the entire project
- `./gradlew test` - Run all tests using JUnit 5
- `./gradlew bootRun` - Run the Spring Boot application locally
- `./gradlew clean` - Clean build artifacts

### Development
- `./gradlew bootJar` - Create executable JAR file
- `./gradlew check` - Run all verification tasks (tests, code quality checks)

### Docker Operations
- `docker-compose up -d` - Start all services in detached mode
- `docker-compose ps` - Check service status
- `docker-compose logs -f [service]` - Follow logs for specific service
- `docker-compose down -v` - Stop and remove all containers with volumes

### K6 Load Testing
- `k6 run k6-tests/enhanced-auto-deployment-test.js` - Run enhanced auto deployment test
- `k6 run k6-tests/phase1-basic-canary.js` - Run basic canary test
- `./k6-tests/run-all-phases.sh` - Run all learning phases

## Architecture

### Core Application Structure
- **Main Application**: `src/main/java/junghun/studycicd/StudycicdApplication.java` - Spring Boot main class with @EnableScheduling
- **Controllers**: RESTful APIs for traffic management, auto deployment, and business metrics
- **Services**: Core business logic including deployment automation and SLI/SLO evaluation
- **Configuration**: Multiple application profiles (legacy/refactored) with metrics collection

### Key Components

#### 1. Traffic Management System
- **TrafficController**: Manual traffic weight adjustment APIs
- **TrafficManagementService**: NGINX configuration management
- **NginxConfigService**: Dynamic configuration reloading

#### 2. Automated Deployment System
- **AutoDeploymentScheduler**: Scheduled task for progressive traffic shifting (5% → 10% → 25% → 50% → 100%)
- **AutoDeploymentController**: APIs for starting/stopping auto deployments
- **DeploymentService**: Core deployment logic with rollback capabilities

#### 3. SLI/SLO Monitoring
- **SliSloEvaluator**: Real-time evaluation of Service Level Indicators/Objectives
- **MetricsService**: Prometheus integration for system metrics
- **Key SLOs**: Error rate < 2%, P95 response time < 1s, Availability > 99.9%

#### 4. Business Metrics & A/B Testing
- **BusinessMetricsController**: Simulates business transactions (orders, signups, logins)
- **Feature Flag System**: Dynamic feature enabling based on version and user groups
- **A/B Test Analytics**: Conversion rate tracking and statistical significance

### Infrastructure Components
- **NGINX**: Load balancer with dynamic weight adjustment and detailed logging
- **Prometheus**: Metrics collection with custom recording and alerting rules
- **Grafana**: Visualization with pre-configured canary deployment dashboards
- **Alertmanager**: Alert routing with webhook-based automatic rollback
- **Loki + Promtail**: Log aggregation and analysis
- **Webhook Handler**: Node.js service for processing rollback webhooks

## Project Configuration

- **Java Version**: 21 (using toolchain)
- **Spring Boot Version**: 3.5.4
- **Build Tool**: Gradle with Wrapper
- **Application Name**: `studycicd` (configured in application.properties)
- **Group**: `junghun`
- **Version**: `0.0.1-SNAPSHOT`

## API Endpoints

### Traffic Management
- `GET /api/traffic/status` - Current traffic distribution
- `POST /api/traffic/adjust` - Manual traffic weight adjustment
- `POST /api/traffic/canary/start` - Start manual canary deployment
- `POST /api/traffic/canary/rollback` - Rollback deployment

### Auto Deployment
- `POST /api/auto-deployment/start` - Start automated progressive deployment
- `POST /api/auto-deployment/stop` - Stop auto deployment
- `GET /api/auto-deployment/status` - Auto deployment status
- `GET /api/auto-deployment/sli-slo` - Current SLI/SLO metrics
- `GET /api/auto-deployment/dashboard` - Comprehensive dashboard data

### Business Metrics
- `POST /api/business/order` - Simulate order transaction
- `POST /api/business/signup` - Simulate user signup
- `POST /api/business/login` - Simulate user login
- `GET /api/business/feature/{name}` - Feature flag status
- `GET /api/business/metrics/summary` - Business metrics summary
- `GET /api/business/ab-test/{testName}` - A/B test results

### Testing Endpoints
- `GET /api/hello` - Basic health check with version info
- `GET /api/test` - Test endpoint with metrics collection
- `GET /api/error-simulation` - Intentional error generation for testing (legacy)
- `GET /api/version` - Application version information

### Error Simulation Endpoints
- `GET /api/error-simulation/random` - Generate random simulated error
- `GET /api/error-simulation/type/{errorType}` - Generate specific error type
- `GET /api/error-simulation/config` - Get current configuration
- `POST /api/error-simulation/periodic/toggle` - Enable/disable periodic errors
- `POST /api/error-simulation/api/toggle` - Enable/disable API errors
- `POST /api/error-simulation/rate` - Set error rates
- `GET /api/error-simulation/statistics` - Get error statistics
- `POST /api/error-simulation/statistics/reset` - Reset statistics
- `GET /api/error-simulation/types` - List all available error types

## Configuration Properties

### Auto Deployment Settings
- `auto-deployment.enabled=true` - Enable/disable auto deployment
- `auto-deployment.stage-wait-minutes=5` - Minutes to wait between stages
- `auto-deployment.evaluation-period-minutes=3` - SLI evaluation period
- `auto-deployment.min-sample-size=100` - Minimum samples before progression

### SLO Thresholds
- `slo.error-rate.threshold=2.0` - Maximum error rate percentage
- `slo.response-time.p95.threshold=1.0` - Maximum P95 response time (seconds)
- `slo.availability.threshold=99.9` - Minimum availability percentage
- `slo.throughput.min-rps=1.0` - Minimum requests per second

## Development Notes

This is a production-ready learning environment that demonstrates:

1. **Automated Progressive Deployment**: Implements real-world canary deployment with SLI/SLO-based decision making
2. **Comprehensive Monitoring**: Full observability stack with metrics, logs, and traces
3. **Business Impact Measurement**: A/B testing and business metrics integration
4. **Failure Handling**: Automatic rollback with webhook integration
5. **Load Testing**: K6-based scenarios covering various deployment phases

The system automatically progresses through deployment stages (5% → 10% → 25% → 50% → 100%) based on SLI/SLO compliance, with automatic rollback on threshold violations.

## Learning Resources

- **Interactive Guide**: `docs/INTERACTIVE_LEARNING_GUIDE.md` - Step-by-step learning path
- **K6 Tests**: `k6-tests/` - Progressive learning scenarios from basic to production-scale
- **Monitoring**: Grafana dashboards at http://localhost:3000 (admin/admin)
- **Metrics**: Prometheus at http://localhost:9090
- **Logs**: Grafana Loki integration for centralized logging

## Important Considerations

- The system is designed for learning and local development
- SLO thresholds are configured for demonstration purposes
- Business metrics are simulated for educational value
- All passwords and secrets are for local development only
- The auto deployment scheduler runs every minute when enabled