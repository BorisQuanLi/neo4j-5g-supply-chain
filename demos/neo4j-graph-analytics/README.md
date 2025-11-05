# Graph Visualization Demo for Jefferies Securities Interview

## Quick Demo Setup (5 minutes)

### Option 1: Use Running Neo4j Container
```bash
# Ensure services are running
docker-compose up -d

# Access Neo4j Browser
open http://localhost:7474
# Login: neo4j / password
```

### Option 2: Neo4j Desktop/Aura (Backup)
1. Download Neo4j Desktop or create Neo4j Aura account
2. Create new database
3. Copy-paste queries from `5g-supply-chain-knowledge-graph.cypher`

## Demo Presentation Flow

### 1. Data Loading (30 seconds)
```cypher
// Copy-paste the CREATE statement from 5g-supply-chain-knowledge-graph.cypher
// Creates 6 companies + 8 relationships
```

### 2. Network Visualization (1 minute)
```cypher
MATCH (n:Company)
OPTIONAL MATCH (n)-[r]-(m:Company)
RETURN n, r, m;
```
**Show**: Complete supply chain network with nodes and relationships

### 3. Frenemy Analysis (1 minute)
```cypher
MATCH (samsung:Company {name: "Samsung Electronics Co Ltd"})-[r]-(apple:Company {name: "Apple Inc"})
RETURN samsung, r, apple;
```
**Explain**: Samsung both competes with AND supplies components to Apple

### 4. Pathfinding Demo (1 minute)
```cypher
MATCH path = shortestPath((apple:Company {name: "Apple Inc"})-[*]-(arm:Company {name: "ARM Holdings plc"}))
RETURN path;
```
**Show**: How Apple connects to ARM through supply chain relationships

### 5. Financial Risk Analysis (1 minute)
```cypher
MATCH (supplier:Company)-[r:SUPPLY_COMPONENTS|MANUFACTURING_PARTNER]-(customer:Company)
WHERE r.contract_value > 5000000000
RETURN supplier, r, customer;
```
**Highlight**: High-value contracts representing systemic risk

## Key Interview Talking Points

### Technical Sophistication
- **Graph Database Expertise**: Complex relationship modeling
- **Financial Domain Knowledge**: Supply chain risk, counterparty analysis
- **Visualization Skills**: Interactive graph exploration
- **Query Optimization**: Efficient pathfinding and aggregation

### Business Value Proposition
- **Risk Management**: Identify single points of failure
- **Competitive Intelligence**: Frenemy relationship analysis
- **Investment Strategy**: Network value vs market cap analysis
- **Regulatory Compliance**: Audit trails and relationship tracking

## Advanced Demo Extensions (if time permits)

### Market Concentration Risk
```cypher
MATCH (c:Company)
WHERE c.is_final_assembler = true
WITH sum(c.market_cap) AS total_market_cap
MATCH (c:Company)
WHERE c.is_final_assembler = true
RETURN c.name AS Company,
       round(100.0 * c.market_cap / total_market_cap, 2) AS MarketShare
ORDER BY MarketShare DESC;
```

### Geographic Risk Distribution
```cypher
MATCH (c:Company)
RETURN c.country AS Country, 
       collect(c.name) AS Companies,
       count(c) AS CompanyCount
ORDER BY CompanyCount DESC;
```

## Backup: Static Screenshots
If live demo fails, screenshots are saved in `demo/screenshots/` directory.