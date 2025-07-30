#!/bin/bash

# 카나리 배포 학습을 위한 통합 테스트 스크립트
# 모든 Phase를 순차적으로 실행하며 학습 시나리오를 제공

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${BASE_DIR}/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 결과 디렉토리 생성
mkdir -p "${RESULTS_DIR}/${TIMESTAMP}"

echo "🚀 Starting Canary Deployment Learning Scenarios"
echo "📁 Results will be saved to: ${RESULTS_DIR}/${TIMESTAMP}"
echo ""

# Phase 1: 기본 카나리 배포
echo "📊 Phase 1.1: Basic Canary Deployment"
echo "==============================================="
echo "Learning objectives:"
echo "- Understand traffic distribution (95% Legacy, 5% Canary)"
echo "- Monitor request routing through logs"
echo "- Track upstream server selection"
echo ""
read -p "Press Enter to start Phase 1.1..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase1-basic.json" \
       "${BASE_DIR}/phase1-basic-canary.js"

echo ""
echo "📊 Phase 1.2: Error Simulation & Debugging"
echo "==============================================="
echo "Learning objectives:"
echo "- Identify errors through LogQL queries"
echo "- Trace request flow using Request ID"
echo "- Analyze error patterns by version"
echo ""
read -p "Press Enter to start Phase 1.2..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase1-error.json" \
       "${BASE_DIR}/phase1-error-simulation.js"

echo ""
echo "🔄 Phase 2.1: Traffic Shifting"
echo "==============================================="
echo "Learning objectives:"
echo "- Simulate gradual traffic increase to canary"
echo "- Monitor performance metrics during transition"
echo "- Understand SLI/SLO measurement"
echo ""
read -p "Press Enter to start Phase 2.1..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase2-traffic.json" \
       "${BASE_DIR}/phase2-traffic-shifting.js"

echo ""
echo "🔄 Phase 2.2: A/B Testing"
echo "==============================================="
echo "Learning objectives:"
echo "- Compare business metrics between versions"
echo "- Analyze user behavior patterns"
echo "- Make data-driven deployment decisions"
echo ""
read -p "Press Enter to start Phase 2.2..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase2-ab.json" \
       "${BASE_DIR}/phase2-ab-testing.js"

echo ""
echo "🔙 Phase 3: Auto Rollback Simulation"
echo "==============================================="
echo "Learning objectives:"
echo "- Trigger automatic rollback on SLO violations"
echo "- Measure recovery time (MTTR)"
echo "- Understand failure detection mechanisms"
echo ""
read -p "Press Enter to start Phase 3..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase3-rollback.json" \
       "${BASE_DIR}/phase3-auto-rollback.js"

echo ""
echo "🎯 Phase 4: Multi-Canary Deployment"
echo "==============================================="
echo "Learning objectives:"
echo "- Deploy multiple canary versions simultaneously"
echo "- Implement user segment-based routing"
echo "- Manage feature flags and A/B experiments"
echo ""
read -p "Press Enter to start Phase 4..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase4-multi.json" \
       "${BASE_DIR}/phase4-multi-canary.js"

echo ""
echo "🌟 Phase 5: Production Load Simulation"
echo "==============================================="
echo "Learning objectives:"
echo "- Handle production-scale traffic"
echo "- Maintain SLO under high load"
echo "- Monitor system behavior during peak usage"
echo ""
read -p "Press Enter to start Phase 5..."

k6 run --out json="${RESULTS_DIR}/${TIMESTAMP}/phase5-production.json" \
       "${BASE_DIR}/phase5-production-load.js"

echo ""
echo "✅ All phases completed!"
echo ""
echo "📈 Next Steps for Learning:"
echo "1. Open Grafana (http://localhost:3000) and explore the logs"
echo "2. Use the provided LogQL queries to analyze results"
echo "3. Check the JSON results in: ${RESULTS_DIR}/${TIMESTAMP}/"
echo "4. Practice writing custom LogQL queries"
echo ""
echo "🎓 Mastery Checklist:"
echo "☐ Can explain traffic distribution patterns"
echo "☐ Can trace individual requests end-to-end"
echo "☐ Can identify and debug deployment issues"
echo "☐ Can implement automated rollback strategies"
echo "☐ Can design complex canary deployment patterns"
echo "☐ Can optimize for production-scale operations"
echo ""
echo "💡 Pro Tips:"
echo "- Run individual phases multiple times to see different patterns"
echo "- Modify the scripts to test your own scenarios"
echo "- Combine with manual testing for deeper understanding"
echo "- Practice writing alerting rules based on the metrics"

# 결과 요약 생성
echo ""
echo "📋 Generating consolidated report..."
cat > "${RESULTS_DIR}/${TIMESTAMP}/learning_summary.md" << EOF
# Canary Deployment Learning Session Summary

**Timestamp**: ${TIMESTAMP}
**Duration**: $(date)

## Completed Phases
- [x] Phase 1.1: Basic Canary Deployment
- [x] Phase 1.2: Error Simulation & Debugging  
- [x] Phase 2.1: Traffic Shifting
- [x] Phase 2.2: A/B Testing
- [x] Phase 3: Auto Rollback Simulation
- [x] Phase 4: Multi-Canary Deployment
- [x] Phase 5: Production Load Simulation

## Key Learning Points
1. **Traffic Management**: Understanding how NGINX distributes traffic between versions
2. **Observability**: Using LogQL to trace requests and identify issues
3. **Reliability**: Implementing automated rollback for failure scenarios
4. **Scalability**: Managing multiple versions and user segments
5. **Performance**: Maintaining SLO under production load

## Recommended Next Steps
1. Practice writing custom LogQL queries
2. Implement your own rollback automation
3. Design canary strategies for your specific use cases
4. Extend monitoring and alerting capabilities

## Resources
- Test Results: $(ls ${RESULTS_DIR}/${TIMESTAMP}/*.json)
- Grafana Dashboard: http://localhost:3000
- Loki Logs: http://localhost:3100
- Prometheus Metrics: http://localhost:9090
EOF

echo "📁 Learning summary saved to: ${RESULTS_DIR}/${TIMESTAMP}/learning_summary.md"
echo ""
echo "🎉 Congratulations! You've completed the comprehensive canary deployment learning path!"