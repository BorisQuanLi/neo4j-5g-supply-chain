# Neo4j 5G Supply Chain Graph Analytics Platform

## Overview

This project implements a comprehensive graph analytics platform for supply chain analysis, leveraging current smartphone market leaders (Apple, Samsung, Xiaomi) based on Q4 2023-Q2 2025 global market share data. The platform demonstrates enterprise-grade graph database technologies, ETL pipelines, and modern AI integration patterns for financial services applications.

## Architecture

### Multi-Language Design
- **Java Spring Boot Service**: REST API layer for graph data access and analytics
- **Python ETL Pipeline**: Data ingestion from external APIs (WIKIDATA, PermID.org) 
- **Neo4j Graph Database**: High-performance graph storage and query engine
- **MCP Agent Framework**: Multi-agent system for AI-powered analytics

## Project Structure

```
neo4j-5g-supply-chain/
├── README.md                           # This file
├── docker/                             # Container orchestration
│   ├── docker-compose.yml             # Multi-service deployment
│   ├── neo4j/                         # Neo4j database configuration
│   └── services/                      # Service-specific Dockerfiles
├── java-spring-service/               # Java Spring Boot microservice
│   ├── pom.xml                        # Maven dependencies
│   ├── src/main/java/com/jefferies/supplychain/
│   │   ├── SupplyChainApplication.java # Main Spring Boot application
│   │   ├── model/                     # Graph entity models
│   │   ├── repository/                # Neo4j data access layer
│   │   ├── service/                   # Business logic layer
│   │   └── controller/                # REST API controllers
│   └── src/main/resources/
│       ├── application.yml            # Spring configuration
│       └── static/                    # Static web resources
├── python-etl/                       # Python ETL pipeline
│   ├── requirements.txt               # Python dependencies
│   ├── src/
│   │   ├── ingest_wikidata.py        # WIKIDATA API integration
│   │   ├── ingest_permid.py          # PermID.org API integration
│   │   ├── entity_matcher.py         # Entity resolution (92% match logic)
│   │   ├── neo4j_client.py           # Neo4j Python driver wrapper
│   │   └── graph_builder.py          # Graph construction logic
│   └── tests/                        # Unit and integration tests
├── mcp-agents/                       # Model Context Protocol agents
│   ├── fraud_detection_agent/        # Financial fraud detection
│   ├── trading_strategy_agent/       # Trading strategy analysis
│   ├── risk_assessment_agent/        # Risk analysis and modeling
│   └── shared/                       # Common MCP utilities
└── docs/                             # Documentation
    ├── api-specification.md          # REST API documentation
    ├── graph-schema.md               # Neo4j schema design
    ├── deployment-guide.md           # Production deployment
    └── mcp-architecture.md           # Multi-agent system design
```

## Key Features

### Graph Analytics Capabilities
- **Pathfinding & Search**: Shortest path algorithms for supply chain alternatives
- **Centrality Analysis**: Identification of critical nodes (e.g., ARM Holdings)
- **Community Detection**: Clustering of related entities and supply networks
- **Real-time Querying**: Cypher-based graph traversal and analytics

### Modern Enhancements (2025)
- **GraphRAG Integration**: LLM-powered knowledge graph construction
- **Vector Search**: Embedding-based similarity search for unstructured data
- **Cloud-Native Architecture**: AWS/Docker deployment ready
- **Multi-Agent AI**: MCP-based intelligent analysis agents

## Technology Stack

### Core Technologies
- **Neo4j 5.x**: Graph database with GDS (Graph Data Science) library
- **Java 17+**: Spring Boot 3.x with Spring Data Neo4j
- **Python 3.9+**: ETL pipeline with neo4j-driver, pandas, requests
- **Docker**: Containerized deployment and orchestration

### AI/ML Integration
- **LangChain**: LLM framework integration for GraphRAG
- **Neo4j Vector Index**: Semantic search capabilities
- **Model Context Protocol (MCP)**: Multi-agent coordination
- **OpenAI/Claude API**: LLM services for knowledge extraction

## Getting Started

📖 **[Quick Start Guide](docs/QUICK_START.md)** | 🏗️ **[Architecture Overview](docs/ARCHITECTURE.md)** | 🎯 **[Use Cases](docs/USE_CASES.md)**

### Prerequisites
- Docker & Docker Compose
- 8GB+ RAM recommended
- Ports 7474, 7687, 8080, 8081 available

### Quick Start
```bash
# Start all services
docker-compose up -d

# Load demo data
./demos/neo4j-graph-analytics/setup-5g-supply-chain-demo.sh

# Access Neo4j Browser
open http://localhost:7474  # neo4j/password
```

### Explore the Knowledge Graph
Once in the Neo4j Browser, use the following queries to explore the knowledge graph of the global 5G smartphone supply chain. Feel free to modify them or write your own to delve deeper.
```cypher
// Samsung-Apple Frenemy: Competitors who depend on each other
MATCH (samsung:Company {name: 'Samsung Electronics Co Ltd'})-[r]-(apple:Company {name: 'Apple Inc'}) 
RETURN samsung, r, apple;

// Complete supply chain network
MATCH (n:Company) OPTIONAL MATCH (n)-[r]-(m:Company) RETURN n, r, m;

// Market leaders by market capitalization
MATCH (c:Company) WHERE c.is_final_assembler = true 
RETURN c.name, c.market_cap, c.country ORDER BY c.market_cap DESC;
```

## Financial Services Applications

This platform demonstrates foundational data engineering techniques that are crucial for various financial services applications. The current implementation showcases:

### 1. Knowledge Graph Construction & Data Integration
- **Entity Resolution**: Merging company data from disparate sources (WIKIDATA, PermID) using fuzzy matching and rule-based logic to create a single, unified view of each entity.
- **Data Ingestion Pipeline**: Building a robust ETL pipeline to ingest and model structured data from external APIs into a graph database.
- **Relationship Analysis**: Modeling and analyzing complex relationships between companies, such as supply chain dependencies, competition, and partnerships.

### 2. Potential Use Cases & Future Directions
The data engineering foundation of this project can be extended to support more advanced financial use cases, including:

- **Trading Strategy**: Enriching the knowledge graph with market data, news sentiment, and research to uncover alpha-generating insights and hidden connections between securities.
- **Risk Management**: Modeling risk propagation through the supply chain, calculating counterparty exposure through multi-hop analysis, and identifying single points of failure.
- **Compliance & Fraud Detection**: Integrating regulatory data to map compliance requirements and using graph algorithms to detect anomalous patterns indicative of fraud.

## Development Workflow

### Multi-Language Considerations
1. **Shared Data Models**: JSON schema definitions for cross-language compatibility
2. **API Contracts**: OpenAPI specifications for service integration
3. **Build Orchestration**: Maven for Java, pip for Python, Docker for deployment
4. **Testing Strategy**: Unit tests per language, integration tests via Docker
5. **Version Management**: Semantic versioning with language-specific tagging

### Key Issues to Resolve
- **Data Consistency**: Ensure entity IDs are consistent across Python ETL and Java API
- **Transaction Management**: Coordinate between ETL writes and API reads
- **Error Handling**: Unified error propagation across language boundaries
- **Monitoring**: Centralized logging and metrics collection
- **Security**: Authentication/authorization across services

## Roadmap: Future Enhancements

This platform provides a solid foundation for a comprehensive graph data engineering and analytics solution. Future development is planned across several key areas:

-   **Advanced Financial Analytics**: Integrating more sophisticated financial models and analytics, such as advanced risk assessment and trading strategy analysis.

-   **CI/CD and Automation**: Implementing a full CI/CD pipeline to automate testing, builds, and deployments, ensuring robustness and rapid iteration.

-   **Cloud-Native Deployment**: Enhancing the deployment architecture with Kubernetes for scalable, resilient, and cloud-agnostic operation.

---

*Enterprise Graph Data Engineering Platform*
*Showcasing Neo4j, Java Spring Boot, Python ETL, and modern AI integration*
