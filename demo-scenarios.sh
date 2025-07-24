#!/bin/bash

# =============================================================================
# Demo Scenarios for Log Analysis System
# Demonstrates real-world usage patterns and workflows
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Print functions
print_status() { echo -e "${BLUE}[DEMO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[SCENARIO]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Simulate typing effect
type_command() {
    local cmd="$1"
    local delay="${2:-0.05}"
    
    echo -n "$ "
    for ((i=0; i<${#cmd}; i++)); do
        echo -n "${cmd:$i:1}"
        sleep "$delay"
    done
    echo ""
}

# Demo scenario 1: New developer onboarding
demo_new_developer() {
    print_header "SCENARIO 1: NEW DEVELOPER ONBOARDING"
    
    echo -e "${CYAN}👨‍💻 Scenario: A new developer joins the team${NC}"
    echo "   - No local Java/Maven/Node.js setup"
    echo "   - Needs to start development quickly"
    echo "   - Wants hot reload and debugging capabilities"
    echo ""
    
    print_warning "Step 1: Quick environment check"
    type_command "./test-deployment.sh"
    echo "✅ Environment validated - ready for development"
    echo ""
    
    print_warning "Step 2: Start development environment"
    type_command "./dev-tools.sh dev"
    echo "✅ Databases started, ready for local development"
    echo ""
    
    print_warning "Step 3: Enable debugging"
    type_command "./dev-tools.sh debug"
    echo "✅ Debug mode enabled on port 5005"
    echo ""
    
    print_warning "Step 4: Monitor system health"
    type_command "./monitor.sh health"
    echo "✅ All services healthy and ready"
    echo ""
    
    print_success "New developer is productive in under 5 minutes!"
    echo ""
}

# Demo scenario 2: Production deployment
demo_production_deployment() {
    print_header "SCENARIO 2: PRODUCTION DEPLOYMENT"
    
    echo -e "${CYAN}🏭 Scenario: Deploying to production environment${NC}"
    echo "   - Need security scanning before deployment"
    echo "   - Multi-environment isolation required"
    echo "   - Performance monitoring essential"
    echo ""
    
    print_warning "Step 1: Security assessment"
    type_command "./security-tools.sh full-scan"
    echo "✅ Security scan completed - no critical vulnerabilities"
    echo ""
    
    print_warning "Step 2: Initialize production environment"
    type_command "./env-manager.sh init"
    echo "✅ Multi-environment configurations created"
    echo ""
    
    print_warning "Step 3: Deploy to production"
    type_command "./env-manager.sh deploy prod"
    echo "✅ Production deployment completed"
    echo ""
    
    print_warning "Step 4: Setup monitoring"
    type_command "./observability.sh deploy"
    echo "✅ Prometheus, Grafana, and Jaeger deployed"
    echo ""
    
    print_warning "Step 5: Performance baseline"
    type_command "./performance.sh benchmark"
    echo "✅ Performance baseline established"
    echo ""
    
    print_success "Production environment is live with full monitoring!"
    echo ""
}

# Demo scenario 3: DevOps troubleshooting
demo_troubleshooting() {
    print_header "SCENARIO 3: DEVOPS TROUBLESHOOTING"
    
    echo -e "${CYAN}🔧 Scenario: System performance issues reported${NC}"
    echo "   - Application response time increased"
    echo "   - Database connections growing"
    echo "   - Need quick diagnosis and resolution"
    echo ""
    
    print_warning "Step 1: System overview"
    type_command "./monitor.sh overview"
    echo "⚠️  High memory usage detected on backend"
    echo ""
    
    print_warning "Step 2: Performance analysis"
    type_command "./performance.sh optimize"
    echo "⚠️  Recommendations: Increase memory limits, check for leaks"
    echo ""
    
    print_warning "Step 3: Database analysis"
    type_command "./db-tools.sh stats"
    echo "⚠️  High connection count, slow queries detected"
    echo ""
    
    print_warning "Step 4: Database maintenance"
    type_command "./db-tools.sh maintenance"
    echo "✅ Database optimized, dead tuples cleaned"
    echo ""
    
    print_warning "Step 5: Scale backend"
    type_command "./performance.sh scale backend 3"
    echo "✅ Backend scaled to 3 replicas"
    echo ""
    
    print_warning "Step 6: Verify resolution"
    type_command "./monitor.sh performance"
    echo "✅ Performance restored to normal levels"
    echo ""
    
    print_success "Issue resolved with zero downtime!"
    echo ""
}

# Demo scenario 4: Cloud migration
demo_cloud_migration() {
    print_header "SCENARIO 4: CLOUD MIGRATION TO KUBERNETES"
    
    echo -e "${CYAN}☸️ Scenario: Migrating from containers to Kubernetes${NC}"
    echo "   - Need cloud-native deployment"
    echo "   - Auto-scaling requirements"
    echo "   - High availability setup"
    echo ""
    
    print_warning "Step 1: Kubernetes prerequisites check"
    type_command "./k8s-deploy.sh check"
    echo "✅ Kubernetes cluster accessible"
    echo ""
    
    print_warning "Step 2: Generate Kubernetes manifests"
    type_command "./k8s-deploy.sh create-manifests"
    echo "✅ K8s manifests created with HPA and ingress"
    echo ""
    
    print_warning "Step 3: Deploy to Kubernetes"
    type_command "./k8s-deploy.sh deploy"
    echo "✅ Application deployed with auto-scaling"
    echo ""
    
    print_warning "Step 4: Verify deployment"
    type_command "./k8s-deploy.sh status"
    echo "✅ All pods running, ingress configured"
    echo ""
    
    print_warning "Step 5: Test auto-scaling"
    type_command "./performance.sh load-test 100 300"
    echo "✅ Auto-scaling triggered, handling increased load"
    echo ""
    
    print_success "Cloud migration completed with auto-scaling!"
    echo ""
}

# Demo scenario 5: Security incident response
demo_security_incident() {
    print_header "SCENARIO 5: SECURITY INCIDENT RESPONSE"
    
    echo -e "${CYAN}🚨 Scenario: Security vulnerability discovered${NC}"
    echo "   - CVE reported in base image"
    echo "   - Need immediate assessment and remediation"
    echo "   - Compliance audit required"
    echo ""
    
    print_warning "Step 1: Immediate vulnerability scan"
    type_command "./security-tools.sh scan"
    echo "⚠️  Critical vulnerability found in base image"
    echo ""
    
    print_warning "Step 2: Security audit"
    type_command "./security-tools.sh audit"
    echo "⚠️  Security configuration issues identified"
    echo ""
    
    print_warning "Step 3: Apply security hardening"
    type_command "./security-tools.sh harden"
    echo "✅ Hardened containers with distroless images"
    echo ""
    
    print_warning "Step 4: Compliance check"
    type_command "./security-tools.sh compliance"
    echo "✅ GDPR, SOC 2, ISO 27001 compliance verified"
    echo ""
    
    print_warning "Step 5: Redeploy with hardened images"
    type_command "./deploy-podman.sh clean && ./deploy-podman.sh"
    echo "✅ System redeployed with security fixes"
    echo ""
    
    print_success "Security incident resolved and compliance maintained!"
    echo ""
}

# Interactive demo menu
interactive_demo() {
    while true; do
        clear
        echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${PURPLE}║                    DEMO SCENARIOS                            ║${NC}"
        echo -e "${PURPLE}║              Log Analysis System                             ║${NC}"
        echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        
        echo -e "${CYAN}📋 Available Demo Scenarios:${NC}"
        echo "  1. 👨‍💻 New Developer Onboarding"
        echo "  2. 🏭 Production Deployment"
        echo "  3. 🔧 DevOps Troubleshooting"
        echo "  4. ☸️ Cloud Migration to Kubernetes"
        echo "  5. 🚨 Security Incident Response"
        echo "  6. 🎬 Run All Scenarios"
        echo "  7. ❌ Exit"
        echo ""
        
        read -p "Select scenario (1-7): " -n 1 -r
        echo ""
        
        case $REPLY in
            1) demo_new_developer ;;
            2) demo_production_deployment ;;
            3) demo_troubleshooting ;;
            4) demo_cloud_migration ;;
            5) demo_security_incident ;;
            6) 
                demo_new_developer
                demo_production_deployment
                demo_troubleshooting
                demo_cloud_migration
                demo_security_incident
                ;;
            7) print_success "Demo completed!"; exit 0 ;;
            *) print_error "Invalid option" ;;
        esac
        
        echo ""
        read -p "Press Enter to continue..."
    done
}

# Main command handler
case "${1:-interactive}" in
    "new-dev")
        demo_new_developer
        ;;
    "production")
        demo_production_deployment
        ;;
    "troubleshoot")
        demo_troubleshooting
        ;;
    "cloud")
        demo_cloud_migration
        ;;
    "security")
        demo_security_incident
        ;;
    "all")
        demo_new_developer
        demo_production_deployment
        demo_troubleshooting
        demo_cloud_migration
        demo_security_incident
        ;;
    "interactive")
        interactive_demo
        ;;
    "help"|"-h"|"--help")
        echo "Demo Scenarios for Log Analysis System"
        echo ""
        echo "Usage: $0 [scenario]"
        echo ""
        echo "Scenarios:"
        echo "  new-dev      - New developer onboarding demo"
        echo "  production   - Production deployment demo"
        echo "  troubleshoot - DevOps troubleshooting demo"
        echo "  cloud        - Cloud migration demo"
        echo "  security     - Security incident response demo"
        echo "  all          - Run all scenarios"
        echo "  interactive  - Interactive demo menu (default)"
        echo "  help         - Show this help"
        ;;
    *)
        print_error "Unknown scenario: $1"
        echo "Use '$0 help' for available scenarios"
        exit 1
        ;;
esac
