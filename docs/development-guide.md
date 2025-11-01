# Supply Chain Graph Analytics - Development Environment

## Quick Start Commands

```bash
# Start core services (Neo4j + Java API)
docker-compose up -d neo4j java-service

# Run ETL pipeline
docker-compose --profile etl run --rm python-etl

# Start with monitoring
docker-compose --profile monitoring up -d

# Start with data analysis tools
docker-compose --profile analysis up -d

# Stop all services
docker-compose down

# Clean up volumes (WARNING: This will delete all data)
docker-compose down -v
```

## Service URLs

- **Java API**: http://localhost:8080
- **Neo4j Browser**: http://localhost:7474 (neo4j/password)
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Grafana**: http://localhost:3000 (admin/admin) - with monitoring profile
- **Jupyter**: http://localhost:8888 (token: supply-chain-analysis) - with analysis profile

## Environment Variables

Create a `.env` file in the project root:

```env
# Neo4j Configuration
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=password

# Java Service Configuration
SPRING_PROFILES_ACTIVE=development
JAVA_OPTS=-Xmx2g -Xms1g

# Python ETL Configuration
LOG_LEVEL=INFO
PYTHONPATH=/app/src

# External API Keys (for production)
WIKIDATA_USER_AGENT=SupplyChainGraphETL/1.0
PERMID_API_KEY=your_permid_api_key_here
OPENAI_API_KEY=your_openai_key_for_llm_features
```

## Development Workflow

### 1. Initial Setup
```bash
# Clone repository
git clone <repository-url>
cd neo4j-5g-supply-chain

# Start development environment
docker-compose up -d
```

### 2. Run ETL Pipeline
```bash
# Run WIKIDATA ingestion
docker-compose --profile etl run --rm python-etl python src/ingest_wikidata.py

# Check ingestion status
curl http://localhost:8080/api/v1/graph-analytics/health
```

### 3. Test Graph Analytics
```bash
# Get companies
curl http://localhost:8080/api/v1/graph-analytics/companies?minMatchScore=0.8

# Find backup supplier routes
curl "http://localhost:8080/api/v1/graph-analytics/pathfinding/backup-supplier?startCompany=Apple%20Inc&endCompany=MediaTek"

# Analyze critical nodes
curl http://localhost:8080/api/v1/graph-analytics/centrality/critical-nodes?topN=10
```

### 4. Development Commands
```bash
# Java development (local)
cd java-spring-service
mvn spring-boot:run

# Python development (local)
cd python-etl
pip install -r requirements.txt
python src/ingest_wikidata.py

# Run tests
cd java-spring-service && mvn test
cd python-etl && pytest tests/

# Code formatting
cd python-etl && black src/ tests/
```

## Troubleshooting

### Neo4j Connection Issues
```bash
# Check Neo4j status
docker-compose logs neo4j

# Restart Neo4j
docker-compose restart neo4j

# Access Neo4j shell
docker-compose exec neo4j cypher-shell -u neo4j -p password
```

### Java Service Issues
```bash
# Check application logs
docker-compose logs java-service

# Check health endpoint
curl http://localhost:8080/actuator/health

# Restart Java service
docker-compose restart java-service
```

### Python ETL Issues
```bash
# Run ETL with debug logging
docker-compose --profile etl run --rm -e LOG_LEVEL=DEBUG python-etl

# Check ETL logs
docker-compose logs python-etl

# Interactive Python session
docker-compose --profile etl run --rm python-etl python
```

## Production Deployment

### AWS Deployment
```bash
# Build for production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

# Deploy to AWS (requires AWS CLI and ECS setup)
# See docs/deployment-guide.md for detailed instructions
```

### Kubernetes Deployment
```bash
# Generate Kubernetes manifests
# See docs/kubernetes/ directory for YAML files
kubectl apply -f docs/kubernetes/
```

## Monitoring and Observability

### Prometheus Metrics
- Java application metrics: http://localhost:8080/actuator/prometheus
- Prometheus UI: http://localhost:9090 (with monitoring profile)

### Grafana Dashboards
- Application metrics dashboard
- Neo4j database metrics
- Business KPI tracking

### Log Aggregation
- Java logs: `./logs/supply-chain-graph-service.log`
- Python logs: `./python-etl/logs/`
- Neo4j logs: Docker volume `neo4j_logs`

## Security Considerations

### Development Security
- Default passwords are used for development
- No SSL/TLS encryption
- Open access to all services

### Production Security
- Change all default passwords
- Enable SSL/TLS for all services
- Implement proper authentication/authorization
- Use secrets management (AWS Secrets Manager, etc.)
- Network security groups and VPC configuration

## Performance Optimization

### Neo4j Tuning
- Heap size: Configured for 4GB max
- Page cache: 2GB for better query performance
- Connection pooling: 50 max connections

### Java Service Tuning
- JVM heap: 2GB max for production workloads
- Connection pool optimization
- Async processing for long-running queries

### Python ETL Optimization
- Batch processing for bulk operations
- Async HTTP clients for API calls
- Rate limiting for external APIs

## Data Management

### Backup and Restore
```bash
# Backup Neo4j data
docker-compose exec neo4j neo4j-admin dump --database=neo4j --to=/backups/graph-backup.dump

# Restore from backup
docker-compose exec neo4j neo4j-admin load --from=/backups/graph-backup.dump --database=neo4j --force
```

### Data Migration
```bash
# Export data for migration
curl http://localhost:8080/api/v1/graph-analytics/export

# Import data from external sources
docker-compose --profile etl run --rm python-etl python src/import_data.py --source external_data.json
```