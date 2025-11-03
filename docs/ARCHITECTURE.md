# System Architecture

## Overview
This project demonstrates a production-ready microservices architecture for graph analytics, specifically designed for complex relationship analysis in enterprise environments.

## Core Components

### Neo4j Enterprise Database
- **Version**: 5.15.0
- **Plugins**: APOC, Graph Data Science (GDS)
- **Configuration**: Optimized for development/production workloads
- **Memory**: 1GB heap, 512MB page cache

### Spring Boot API Service
- **Framework**: Spring Boot with Spring Data Neo4j
- **Purpose**: RESTful API for graph data access
- **Features**: Health checks, dependency management
- **Port**: 8080

### Python ETL Service
- **Framework**: FastAPI with Neo4j Python driver
- **Purpose**: Async data processing and ingestion
- **Features**: Batch processing, data validation
- **Port**: 8081

## Network Architecture
- **Container Network**: Custom bridge (172.20.0.0/16)
- **Service Discovery**: Docker Compose networking
- **Health Monitoring**: Automatic restart policies
- **Data Persistence**: Named volumes for durability

## Deployment Profiles
- **Development**: Single-node setup with resource optimization
- **Production**: Scalable with external load balancers
- **Testing**: Isolated environments with test data

## Security Features
- Standardized authentication
- Procedure allowlists
- Network isolation
- Secure inter-service communication