# Quick Start Guide

## Prerequisites
- Docker and Docker Compose
- 8GB+ RAM recommended
- Ports 7474, 7687, 8080, 8081 available

## One-Command Setup
```bash
# Start the complete environment
docker-compose up -d

# Verify services are running
docker-compose ps
```

## Access Points
- **Neo4j Browser**: http://localhost:7474 (neo4j/password)
- **Spring Boot API**: http://localhost:8080
- **Python ETL Service**: http://localhost:8081

## Demo Data Setup
```bash
# Load sample supply chain data
./demos/neo4j-graph-analytics/setup-5g-supply-chain-demo.sh

# Interactive exploration
./demos/neo4j-graph-analytics/interactive-cypher-demo.sh
```

## Key Queries
```cypher
// Complete network visualization
MATCH (n:Company) OPTIONAL MATCH (n)-[r]-(m:Company) RETURN n, r, m;

// Shortest path analysis
MATCH path = shortestPath((apple:Company {name: "Apple Inc"})-[*]-(arm:Company {name: "ARM Holdings plc"})) RETURN path;
```

## Architecture Overview
- **Neo4j Enterprise**: Graph database with GDS plugins
- **Spring Boot**: RESTful API microservice
- **Python ETL**: Data processing pipeline
- **Docker Compose**: Container orchestration