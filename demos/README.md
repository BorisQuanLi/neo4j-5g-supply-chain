# Graph Analytics Demonstrations

This directory contains interactive demonstrations of advanced graph database analytics capabilities, specifically designed to showcase advanced graph database technical expertise for financial services applications.

## Demo Structure

```
demos/
└── neo4j-graph-analytics/           # Neo4j graph database demonstrations
    ├── GRAPH_ANALYTICS_GUIDE.md     # Comprehensive technical guide
    ├── README.md                     # Demo-specific documentation
    ├── setup-5g-supply-chain-demo.sh # Quick setup script
    ├── interactive-cypher-demo.sh    # Interactive query exploration
    ├── load-demo-data.cypher         # Sample data loading script
    └── 5g-supply-chain-knowledge-graph.cypher     # Sample knowledge graph queries
```

## Quick Start (5 minutes)

This guide will get the complete graph analytics platform running on your local machine.

### Prerequisites
- Docker & Docker Compose

### 1. Start the Platform
This command starts all services, including the Neo4j database and the Java API. The first time you run this, Docker will build the necessary images.

```bash
docker-compose up -d
```

### 2. Load Demo Data
This script populates the Neo4j database with the 5G supply chain dataset.

```bash
./demos/neo4j-graph-analytics/setup-5g-supply-chain-demo.sh
```

### 3. Explore the Graph
You have two options for exploring the data:

#### Option A: Neo4j Browser (Recommended)
- **Access**: Open http://localhost:7474 in your web browser.
- **Login**: Use `neo4j` for the username and `password` for the password.
- **Queries**: Run the queries from `demos/neo4j-graph-analytics/5g-supply-chain-knowledge-graph.cypher` to visualize the supply chain network.

#### Option B: Interactive Command-Line Demo
For a guided tour through the data using Cypher queries in your terminal:
```bash
./demos/neo4j-graph-analytics/interactive-cypher-demo.sh
```

## Demo Capabilities

### 1. **Network Topology Analysis**
- Complete supply chain visualization
- Node degree and centrality analysis
- Network density metrics

### 2. **Advanced Relationship Patterns**
- **Frenemy Analysis**: Companies that both compete and partner (Apple ↔ Samsung)
- **Multi-hop Pathfinding**: Alternative supply routes and dependencies
- **Complex Relationship Scoring**: Multi-dimensional business connections

### 3. **Financial Risk Analytics**
- **Concentration Risk**: Market share and dependency analysis
- **Systemic Risk**: Single points of failure identification
- **Geographic Risk**: Cross-border relationship mapping
- **Counterparty Risk**: High-value contract exposure analysis

### 4. **Business Intelligence Queries**
- **Investment Analysis**: Network value vs market capitalization
- **Competitive Intelligence**: Market maker identification
- **M&A Target Analysis**: Undervalued network positions

## Technical Sophistication Demonstrated

### Graph Database Expertise
- Complex Cypher query optimization
- Multi-relationship pattern matching
- Graph algorithm integration (PageRank, community detection)
- Performance indexing strategies

### Financial Domain Knowledge
- Supply chain → financial network analogies
- Risk propagation modeling
- Regulatory compliance patterns
- Trading strategy intelligence

### Production Readiness
- Containerized deployment with Docker
- Automated data loading and setup
- Interactive user experience design
- Comprehensive documentation

## Business Value Proposition

This demonstration platform showcases how graph database technologies can be applied to:

- **Trading Networks**: Counterparty relationship analysis
- **Risk Management**: Systemic risk identification and monitoring  
- **Fraud Detection**: Anomalous relationship pattern recognition
- **Regulatory Compliance**: Cross-jurisdictional relationship tracking
- **Investment Strategy**: Network-based valuation and opportunity identification

## Demo Flow

1. **Setup**: `./demos/neo4j-graph-analytics/setup-5g-supply-chain-demo.sh`
2. **Network Visualization**: Complete supply chain topology
3. **Relationship Analysis**: Competition and partnership dynamics (Apple-Samsung, Samsung-Xiaomi)
4. **Pathfinding**: Alternative supply routes discovery
5. **Risk Assessment**: Critical dependency identification
6. **Interactive Exploration**: Custom queries and advanced analytics

This platform demonstrates enterprise-grade graph database capabilities for complex network analysis and business intelligence applications.