#!/bin/bash

# Supply Chain Graph Analytics Platform - Build and Development Script
# This script provides a unified interface for building, testing, and running the platform

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to check if docker-compose is available
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        print_error "docker-compose is not installed. Please install it and try again."
        exit 1
    fi
}

# Function to wait for service to be healthy
wait_for_service() {
    local service_name=$1
    local max_attempts=30
    local attempt=1

    print_info "Waiting for $service_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps | grep "$service_name" | grep -q "healthy"; then
            print_success "$service_name is healthy"
            return 0
        fi
        
        print_info "Attempt $attempt/$max_attempts: $service_name not ready yet..."
        sleep 10
        attempt=$((attempt + 1))
    done
    
    print_error "$service_name failed to become healthy within expected time"
    return 1
}

# Function to build services
build_services() {
    print_info "Building all services..."
    
    # Build Java service
    print_info "Building Java Spring Boot service..."
    cd java-spring-service
    ./mvnw clean package -DskipTests
    cd ..
    
    # Build Docker images
    print_info "Building Docker images..."
    docker-compose build
    
    print_success "All services built successfully"
}

# Function to start core services
start_core() {
    print_info "Starting core services (Neo4j + Java API)..."
    docker-compose up -d neo4j java-service
    
    # Wait for services to be healthy
    wait_for_service "neo4j"
    wait_for_service "java-service"
    
    print_success "Core services are running"
    print_info "Neo4j Browser: http://localhost:7474 (neo4j/password)"
    print_info "Java API: http://localhost:8080"
    print_info "API Documentation: http://localhost:8080/swagger-ui.html"
}

# Function to run ETL pipeline
run_etl() {
    print_info "Running ETL pipeline..."
    
    # Ensure core services are running
    if ! docker-compose ps | grep -q "neo4j.*healthy"; then
        print_warning "Neo4j not running. Starting core services first..."
        start_core
    fi
    
    # Run WIKIDATA ingestion
    print_info "Running WIKIDATA ingestion..."
    docker-compose --profile etl run --rm python-etl python src/ingest_wikidata.py
    
    # Create sample data if needed
    print_info "Creating sample data..."
    docker-compose --profile etl run --rm python-etl python src/neo4j_client.py
    
    print_success "ETL pipeline completed"
}

# Function to run tests
run_tests() {
    print_info "Running tests..."
    
    # Java tests
    print_info "Running Java tests..."
    cd java-spring-service
    ./mvnw test
    cd ..
    
    # Python tests
    print_info "Running Python tests..."
    docker-compose --profile etl run --rm python-etl pytest tests/ -v
    
    print_success "All tests passed"
}

# Function to start monitoring
start_monitoring() {
    print_info "Starting monitoring services..."
    docker-compose --profile monitoring up -d prometheus grafana
    
    print_success "Monitoring services started"
    print_info "Prometheus: http://localhost:9090"
    print_info "Grafana: http://localhost:3000 (admin/admin)"
}

# Function to start analysis tools
start_analysis() {
    print_info "Starting analysis tools..."
    docker-compose --profile analysis up -d jupyter
    
    print_success "Analysis tools started"
    print_info "Jupyter: http://localhost:8888 (token: supply-chain-analysis)"
}

# Function to show status
show_status() {
    print_info "Service Status:"
    docker-compose ps
    
    echo ""
    print_info "Service Health Checks:"
    
    # Check Neo4j
    if curl -s http://localhost:7474 > /dev/null; then
        print_success "Neo4j Browser: http://localhost:7474"
    else
        print_warning "Neo4j Browser: Not accessible"
    fi
    
    # Check Java API
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        print_success "Java API: http://localhost:8080"
    else
        print_warning "Java API: Not accessible"
    fi
    
    # Check if we have data
    if curl -s "http://localhost:8080/api/v1/graph-analytics/companies?minMatchScore=0.8" | grep -q "permid"; then
        print_success "Graph database contains data"
    else
        print_warning "Graph database appears to be empty. Run ETL pipeline."
    fi
}

# Function to demo the platform
run_demo() {
    print_info "Running platform demonstration..."
    
    # Ensure services are running with data
    start_core
    run_etl
    
    echo ""
    print_info "=== DEMO: Graph Analytics Platform ==="
    echo ""
    
    # Demo 1: Get companies
    print_info "Demo 1: Getting high-confidence companies..."
    curl -s "http://localhost:8080/api/v1/graph-analytics/companies?minMatchScore=0.8" | jq '.[0:3]' || echo "JSON parsing not available"
    echo ""
    
    # Demo 2: Pathfinding
    print_info "Demo 2: Finding backup supplier routes..."
    curl -s "http://localhost:8080/api/v1/graph-analytics/pathfinding/backup-supplier?startCompany=Apple%20Inc&endCompany=MediaTek" | jq '.[0:2]' || echo "JSON parsing not available"
    echo ""
    
    # Demo 3: Critical nodes
    print_info "Demo 3: Analyzing critical nodes..."
    curl -s "http://localhost:8080/api/v1/graph-analytics/centrality/critical-nodes?topN=5" | jq '.[0:3]' || echo "JSON parsing not available"
    echo ""
    
    # Demo 4: Frenemy relationships
    print_info "Demo 4: Analyzing frenemy relationships..."
    curl -s "http://localhost:8080/api/v1/graph-analytics/analytics/frenemy-relationships" | jq '.[0:2]' || echo "JSON parsing not available"
    echo ""
    
    print_success "Demo completed! Check the output above for graph analytics results."
    print_info "Visit http://localhost:8080/swagger-ui.html for interactive API documentation"
}

# Function to clean up
cleanup() {
    print_info "Cleaning up services and data..."
    docker-compose down -v
    docker system prune -f
    print_success "Cleanup completed"
}

# Function to show logs
show_logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_info "Showing logs for all services..."
        docker-compose logs --tail=50 -f
    else
        print_info "Showing logs for $service..."
        docker-compose logs --tail=50 -f "$service"
    fi
}

# Function to show help
show_help() {
    echo "Supply Chain Graph Analytics Platform - Build and Development Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build       Build all services and Docker images"
    echo "  start       Start core services (Neo4j + Java API)"
    echo "  etl         Run ETL pipeline to ingest data"
    echo "  test        Run all tests (Java + Python)"
    echo "  monitor     Start monitoring services (Prometheus + Grafana)"
    echo "  analysis    Start analysis tools (Jupyter)"
    echo "  status      Show service status and health"
    echo "  demo        Run complete demonstration of the platform"
    echo "  logs [svc]  Show logs (optionally for specific service)"
    echo "  cleanup     Stop all services and clean up data"
    echo "  help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build && $0 start && $0 etl    # Full setup"
    echo "  $0 demo                           # Quick demonstration"
    echo "  $0 logs java-service              # Show Java service logs"
    echo "  $0 cleanup                        # Clean up everything"
    echo ""
    echo "For more information, see docs/development-guide.md"
}

# Main script logic
main() {
    # Check prerequisites
    check_docker
    check_docker_compose
    
    # Parse command
    case "${1:-help}" in
        "build")
            build_services
            ;;
        "start")
            start_core
            ;;
        "etl")
            run_etl
            ;;
        "test")
            run_tests
            ;;
        "monitor")
            start_monitoring
            ;;
        "analysis")
            start_analysis
            ;;
        "status")
            show_status
            ;;
        "demo")
            run_demo
            ;;
        "logs")
            show_logs "$2"
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"--help"|"-h")
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"