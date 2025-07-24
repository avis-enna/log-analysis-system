#!/bin/bash

# =============================================================================
# Kubernetes Deployment for Log Analysis System
# Alternative to Podman deployment using Kubernetes
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

# Configuration
PROJECT_NAME="log-analyzer"
NAMESPACE="log-analyzer"
K8S_DIR="k8s-manifests"

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Check prerequisites
check_prerequisites() {
    print_header "CHECKING PREREQUISITES"
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed"
        echo "Install kubectl: https://kubernetes.io/docs/tasks/tools/"
        return 1
    fi
    
    # Check cluster connection
    if ! kubectl cluster-info &>/dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        echo "Make sure you have a running cluster and valid kubeconfig"
        return 1
    fi
    
    print_success "Prerequisites check passed"
    
    # Show cluster info
    echo ""
    echo -e "${CYAN}🔍 Cluster Information:${NC}"
    kubectl cluster-info | head -2
}

# Create Kubernetes manifests
create_manifests() {
    print_header "CREATING KUBERNETES MANIFESTS"
    
    mkdir -p "$K8S_DIR"
    
    # Namespace
    cat > "$K8S_DIR/namespace.yaml" << EOF
apiVersion: v1
kind: Namespace
metadata:
  name: $NAMESPACE
  labels:
    app: $PROJECT_NAME
EOF
    
    # ConfigMap for application configuration
    cat > "$K8S_DIR/configmap.yaml" << EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${PROJECT_NAME}-config
  namespace: $NAMESPACE
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: log-analyzer-k8s
      datasource:
        url: jdbc:postgresql://postgres-service:5432/loganalyzer
        username: loguser
        password: logpass123
      data:
        redis:
          host: redis-service
          port: 6379
    logging:
      level:
        com.loganalyzer: INFO
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
EOF
    
    # Secrets
    cat > "$K8S_DIR/secrets.yaml" << EOF
apiVersion: v1
kind: Secret
metadata:
  name: ${PROJECT_NAME}-secrets
  namespace: $NAMESPACE
type: Opaque
data:
  postgres-password: $(echo -n "logpass123" | base64)
  redis-password: $(echo -n "" | base64)
EOF
    
    # PostgreSQL Deployment
    cat > "$K8S_DIR/postgres.yaml" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
        - name: POSTGRES_DB
          value: loganalyzer
        - name: POSTGRES_USER
          value: loguser
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ${PROJECT_NAME}-secrets
              key: postgres-password
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - loguser
            - -d
            - loganalyzer
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - loguser
            - -d
            - loganalyzer
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: $NAMESPACE
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: $NAMESPACE
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
EOF
    
    # Redis Deployment
    cat > "$K8S_DIR/redis.yaml" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        command: ["redis-server", "--appendonly", "yes"]
        volumeMounts:
        - name: redis-storage
          mountPath: /data
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: redis-storage
        persistentVolumeClaim:
          claimName: redis-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: $NAMESPACE
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: $NAMESPACE
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
EOF
    
    # Backend Deployment
    cat > "$K8S_DIR/backend.yaml" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: $NAMESPACE
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: ${PROJECT_NAME}-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: k8s
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
      volumes:
      - name: config-volume
        configMap:
          name: ${PROJECT_NAME}-config
      - name: logs-volume
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: $NAMESPACE
spec:
  selector:
    app: backend
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: backend-ingress
  namespace: $NAMESPACE
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: api.log-analyzer.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 8080
EOF
    
    # Frontend Deployment
    cat > "$K8S_DIR/frontend.yaml" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: $NAMESPACE
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: ${PROJECT_NAME}-frontend:latest
        ports:
        - containerPort: 3000
        env:
        - name: REACT_APP_API_URL
          value: http://api.log-analyzer.local
        livenessProbe:
          httpGet:
            path: /
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /
            port: 3000
          initialDelaySeconds: 10
          periodSeconds: 10
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: frontend-service
  namespace: $NAMESPACE
spec:
  selector:
    app: frontend
  ports:
  - port: 3000
    targetPort: 3000
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: frontend-ingress
  namespace: $NAMESPACE
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: log-analyzer.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 3000
EOF
    
    # Horizontal Pod Autoscaler
    cat > "$K8S_DIR/hpa.yaml" << EOF
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: $NAMESPACE
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
EOF
    
    print_success "Kubernetes manifests created in $K8S_DIR/"
}

# Deploy to Kubernetes
deploy_k8s() {
    print_header "DEPLOYING TO KUBERNETES"
    
    # Apply manifests in order
    print_status "Creating namespace..."
    kubectl apply -f "$K8S_DIR/namespace.yaml"
    
    print_status "Creating secrets and config..."
    kubectl apply -f "$K8S_DIR/secrets.yaml"
    kubectl apply -f "$K8S_DIR/configmap.yaml"
    
    print_status "Deploying databases..."
    kubectl apply -f "$K8S_DIR/postgres.yaml"
    kubectl apply -f "$K8S_DIR/redis.yaml"
    
    print_status "Waiting for databases to be ready..."
    kubectl wait --for=condition=ready pod -l app=postgres -n "$NAMESPACE" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=redis -n "$NAMESPACE" --timeout=300s
    
    print_status "Deploying applications..."
    kubectl apply -f "$K8S_DIR/backend.yaml"
    kubectl apply -f "$K8S_DIR/frontend.yaml"
    
    print_status "Setting up autoscaling..."
    kubectl apply -f "$K8S_DIR/hpa.yaml"
    
    print_status "Waiting for applications to be ready..."
    kubectl wait --for=condition=ready pod -l app=backend -n "$NAMESPACE" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=frontend -n "$NAMESPACE" --timeout=300s
    
    print_success "Deployment completed!"
    show_k8s_status
}

# Show Kubernetes deployment status
show_k8s_status() {
    print_header "KUBERNETES DEPLOYMENT STATUS"
    
    echo -e "${CYAN}📊 Pods Status:${NC}"
    kubectl get pods -n "$NAMESPACE" -o wide
    
    echo ""
    echo -e "${CYAN}🌐 Services:${NC}"
    kubectl get services -n "$NAMESPACE"
    
    echo ""
    echo -e "${CYAN}🔗 Ingresses:${NC}"
    kubectl get ingress -n "$NAMESPACE"
    
    echo ""
    echo -e "${CYAN}📈 HPA Status:${NC}"
    kubectl get hpa -n "$NAMESPACE"
    
    echo ""
    echo -e "${CYAN}🌐 Access URLs:${NC}"
    echo -e "  Frontend:  ${YELLOW}http://log-analyzer.local${NC}"
    echo -e "  Backend:   ${YELLOW}http://api.log-analyzer.local${NC}"
    echo ""
    echo -e "${CYAN}💡 Note:${NC} Add these entries to your /etc/hosts file:"
    echo "  127.0.0.1 log-analyzer.local"
    echo "  127.0.0.1 api.log-analyzer.local"
}

# Scale deployment
scale_k8s() {
    local component="$1"
    local replicas="$2"
    
    if [[ -z "$component" || -z "$replicas" ]]; then
        print_error "Usage: scale <component> <replicas>"
        echo "Components: backend, frontend"
        return 1
    fi
    
    print_header "SCALING $component TO $replicas REPLICAS"
    
    kubectl scale deployment "$component" --replicas="$replicas" -n "$NAMESPACE"
    
    print_success "Scaling initiated"
    kubectl get deployment "$component" -n "$NAMESPACE"
}

# Clean up Kubernetes deployment
clean_k8s() {
    print_header "CLEANING KUBERNETES DEPLOYMENT"
    
    print_warning "This will delete the entire $NAMESPACE namespace and all resources!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Clean cancelled"
        return 0
    fi
    
    print_status "Deleting namespace $NAMESPACE..."
    kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
    
    print_success "Kubernetes deployment cleaned"
}

# Main command handler
case "${1:-help}" in
    "check")
        check_prerequisites
        ;;
    "create-manifests")
        create_manifests
        ;;
    "deploy")
        check_prerequisites
        create_manifests
        deploy_k8s
        ;;
    "status")
        show_k8s_status
        ;;
    "scale")
        scale_k8s "$2" "$3"
        ;;
    "clean")
        clean_k8s
        ;;
    "help"|"-h"|"--help")
        echo "Kubernetes Deployment for Log Analysis System"
        echo ""
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  check           - Check prerequisites (kubectl, cluster)"
        echo "  create-manifests - Create Kubernetes manifest files"
        echo "  deploy          - Deploy complete stack to Kubernetes"
        echo "  status          - Show deployment status"
        echo "  scale <component> <replicas> - Scale deployment"
        echo "  clean           - Clean up deployment"
        echo "  help            - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 check                    # Check prerequisites"
        echo "  $0 deploy                   # Deploy to Kubernetes"
        echo "  $0 scale backend 5          # Scale backend to 5 replicas"
        echo "  $0 status                   # Show deployment status"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
