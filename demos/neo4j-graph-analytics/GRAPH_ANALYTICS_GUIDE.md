# Querying the 5G Smartphone Global Supply Chain in Neo4j

## Overview

This guide demonstrates advanced graph database analytics on a real-world 5G smartphone supply chain network using Neo4j. The implementation showcases the technical capabilities required for the **Jefferies Securities Graph Data Engineer** position, with direct applications to financial services graph analytics.

## Business Context: From Supply Chain to Financial Networks

The 5G supply chain graph serves as a proxy for complex financial networks:

- **Supply Chain Relationships** → **Trading Counterparty Networks**
- **Component Dependencies** → **Financial Instrument Dependencies** 
- **Frenemy Dynamics** → **Market Maker Relationships**
- **Geographic Risk** → **Regulatory Jurisdiction Risk**
- **Concentration Risk** → **Systemic Financial Risk**

## Graph Schema

### Nodes: Company Entities
```cypher
(:Company {
  permid: Long,           // Thomson Reuters Permanent Identifier
  name: String,           // Company legal name
  ticker: String,         // Stock ticker symbol
  country: String,        // Headquarters country
  industry: String,       // Industry classification
  business_type: String,  // Business model type
  is_final_assembler: Boolean,  // OEM vs supplier classification
  market_cap: Long,       // Market capitalization (USD)
  revenue: Long,          // Annual revenue (USD)
  match_score: Double     // Entity resolution confidence (0.0-1.0)
})
```

### Relationships: Business Connections
```cypher
// Supply chain relationships
(:Company)-[:SUPPLY_COMPONENTS {
  component_type: String,
  strength: Double,
  contract_value: Long,
  exclusivity: Boolean
}]->(:Company)

// Manufacturing partnerships
(:Company)-[:MANUFACTURING_PARTNER {
  component_type: String,
  strength: Double,
  contract_value: Long,
  process_node: String
}]->(:Company)

// IP licensing relationships
(:Company)-[:DESIGN_CHIPS_FOR {
  component_type: String,
  strength: Double,
  license_value: Long,
  architecture: String
}]->(:Company)

// Competition relationships
(:Company)-[:COMPETES_WITH {
  market_segment: String,
  strength: Double,
  competition_intensity: String,
  market_overlap: Double
}]->(:Company)
```

## Core Graph Analytics Patterns

### 1. Network Topology Analysis
**Business Use Case**: Understanding market structure and interconnectedness

```cypher
// Complete network visualization
MATCH (n:Company)
OPTIONAL MATCH (n)-[r]-(m:Company)
RETURN n, r, m;

// Network density metrics
MATCH (c:Company)
OPTIONAL MATCH (c)-[r]-()
WITH c, count(r) AS degree
RETURN 
  count(c) AS total_nodes,
  sum(degree)/2 AS total_edges,
  avg(degree) AS avg_degree,
  max(degree) AS max_degree,
  min(degree) AS min_degree;
```

### 2. Pathfinding & Route Discovery
**Business Use Case**: Alternative trading routes, backup counterparties

```cypher
// Shortest path between entities
MATCH path = shortestPath(
  (start:Company {name: "Apple Inc"})-[*]-(end:Company {name: "ARM Holdings plc"})
)
RETURN path;

// All paths within hop limit (risk propagation analysis)
MATCH path = (start:Company {name: "Apple Inc"})-[*1..3]-(end:Company {name: "MediaTek Inc"})
RETURN [node IN nodes(path) | node.name] AS path_companies,
       length(path) AS path_length;
```

### 3. Centrality Analysis (Critical Node Identification)
**Business Use Case**: Systemic risk identification, key institution analysis

```cypher
// Degree centrality (connection count)
MATCH (c:Company)-[r]-()
WITH c, count(r) AS degree
RETURN c.name AS company, degree
ORDER BY degree DESC LIMIT 10;

// Weighted centrality (contract value importance)
MATCH (c:Company)-[r]-()
WHERE r.contract_value IS NOT NULL
WITH c, sum(r.contract_value) AS total_exposure
RETURN c.name AS company, 
       total_exposure,
       round(total_exposure / c.market_cap * 100, 2) AS exposure_ratio
ORDER BY exposure_ratio DESC;
```

### 4. Community Detection & Clustering
**Business Use Case**: Market segmentation, trading cluster identification

```cypher
// Geographic clustering
MATCH (c:Company)
WITH c.country AS country, collect(c) AS companies
RETURN country, 
       [comp IN companies | comp.name] AS company_names,
       size(companies) AS cluster_size
ORDER BY cluster_size DESC;

// Business type clustering
MATCH (c:Company)
WITH c.business_type AS type, collect(c) AS companies
RETURN type,
       [comp IN companies | comp.name] AS company_names,
       sum([comp IN companies | comp.market_cap]) AS total_market_cap
ORDER BY total_market_cap DESC;
```

### 5. Complex Relationship Analysis (Frenemy Patterns)
**Business Use Case**: Market maker dynamics, competitive intelligence

```cypher
// Competitors who are also partners (frenemy analysis)
MATCH (c1:Company)-[:COMPETES_WITH]->(c2:Company)
MATCH (c1)-[partnership:SUPPLY_COMPONENTS|MANUFACTURING_PARTNER]-(c2)
RETURN c1.name AS company1,
       c2.name AS company2,
       "FRENEMY" AS relationship_type,
       partnership.component_type AS partnership_area;

// Multi-relationship complexity scoring
MATCH (c1:Company)-[r]-(c2:Company)
WITH c1, c2, collect(type(r)) AS relationship_types, count(r) AS relationship_count
WHERE relationship_count > 1
RETURN c1.name AS company1,
       c2.name AS company2,
       relationship_types,
       relationship_count AS complexity_score
ORDER BY complexity_score DESC;
```

### 6. Risk & Vulnerability Assessment
**Business Use Case**: Counterparty risk, systemic risk monitoring

```cypher
// Single point of failure analysis
MATCH (supplier:Company)-[:SUPPLY_COMPONENTS]->(customer:Company)
WITH customer, count(supplier) AS supplier_count
WHERE supplier_count = 1
MATCH (supplier:Company)-[:SUPPLY_COMPONENTS]->(customer)
MATCH (customer)-[:SUPPLY_COMPONENTS|MANUFACTURING_PARTNER]->(downstream:Company)
RETURN customer.name AS vulnerable_entity,
       supplier.name AS critical_dependency,
       count(downstream) AS downstream_impact
ORDER BY downstream_impact DESC;

// Concentration risk analysis
MATCH (c:Company)
WHERE c.is_final_assembler = true
WITH sum(c.market_cap) AS total_market_cap
MATCH (c:Company)
WHERE c.is_final_assembler = true
RETURN c.name AS company,
       round(100.0 * c.market_cap / total_market_cap, 2) AS market_share
ORDER BY market_share DESC;
```

### 7. Financial Analytics Patterns
**Business Use Case**: Investment analysis, M&A target identification

```cypher
// High network value vs low market cap (undervalued network positions)
MATCH (c:Company)-[r]-(other:Company)
WHERE r.contract_value IS NOT NULL
WITH c, 
     count(r) AS network_size,
     sum(r.contract_value) AS network_value
WHERE c.market_cap < 100000000000 AND network_size >= 2
RETURN c.name AS company,
       c.market_cap AS market_cap,
       network_value,
       round(network_value / c.market_cap, 2) AS value_efficiency
ORDER BY value_efficiency DESC;

// Market influence vs financial size analysis
MATCH (c:Company)-[r]-()
WITH c, count(r) AS connections
RETURN c.name AS company,
       c.market_cap AS market_cap,
       connections AS network_influence,
       round(connections * 1000000000.0 / c.market_cap, 4) AS influence_per_billion
ORDER BY influence_per_billion DESC;
```

## Advanced Query Patterns

### Temporal Analysis (Time-Series Extensions)
```cypher
// Relationship evolution over time (requires temporal data)
MATCH (c1:Company)-[r]-(c2:Company)
WHERE r.start_date IS NOT NULL
RETURN c1.name, c2.name, type(r), r.start_date, r.end_date
ORDER BY r.start_date DESC;
```

### Graph Algorithm Integration
```cypher
// PageRank centrality (requires GDS library)
CALL gds.pageRank.stream('supply_chain_graph')
YIELD nodeId, score
RETURN gds.util.asNode(nodeId).name AS company, score
ORDER BY score DESC LIMIT 10;

// Community detection (Louvain algorithm)
CALL gds.louvain.stream('supply_chain_graph')
YIELD nodeId, communityId
RETURN communityId, collect(gds.util.asNode(nodeId).name) AS community_members
ORDER BY communityId;
```

## Performance Optimization

### Indexing Strategy
```cypher
// Essential indexes for query performance
CREATE INDEX company_permid_index IF NOT EXISTS FOR (c:Company) ON (c.permid);
CREATE INDEX company_name_index IF NOT EXISTS FOR (c:Company) ON (c.name);
CREATE INDEX company_country_index IF NOT EXISTS FOR (c:Company) ON (c.country);
CREATE INDEX relationship_value_index IF NOT EXISTS FOR ()-[r]-() ON (r.contract_value);
```

### Query Optimization Patterns
```cypher
// Use LIMIT for large result sets
MATCH (c:Company)-[r]-(other:Company)
RETURN c.name, count(r) AS connections
ORDER BY connections DESC
LIMIT 20;

// Use WHERE clauses early in the query
MATCH (c:Company)
WHERE c.market_cap > 100000000000  // Filter early
MATCH (c)-[r]-(other:Company)
RETURN c.name, count(r);
```

## Integration with Financial Services

### Fraud Detection Patterns
```cypher
// Unusual relationship patterns (anomaly detection)
MATCH (c:Company)-[r]-(other:Company)
WITH c, count(DISTINCT type(r)) AS relationship_types, count(r) AS total_relationships
WHERE relationship_types > 3 OR total_relationships > 10
RETURN c.name AS potentially_suspicious,
       relationship_types,
       total_relationships;
```

### Regulatory Compliance Queries
```cypher
// Cross-border relationship mapping (regulatory jurisdiction analysis)
MATCH (c1:Company)-[r]-(c2:Company)
WHERE c1.country <> c2.country
RETURN c1.country AS country1, c2.country AS country2, 
       count(r) AS cross_border_relationships
ORDER BY cross_border_relationships DESC;
```

### Trading Strategy Intelligence
```cypher
// Market maker identification (high connectivity + competition)
MATCH (c:Company)-[:COMPETES_WITH]-(competitor:Company)
MATCH (c)-[supply:SUPPLY_COMPONENTS|MANUFACTURING_PARTNER]-()
WITH c, count(DISTINCT competitor) AS competitors, count(supply) AS supply_relationships
WHERE competitors >= 2 AND supply_relationships >= 2
RETURN c.name AS potential_market_maker,
       competitors,
       supply_relationships;
```

## Demo Execution

### Quick Start
```bash
# Setup and load data
./demos/neo4j-graph-analytics/setup-5g-supply-chain-demo.sh

# Interactive exploration
./demos/neo4j-graph-analytics/interactive-cypher-demo.sh

# Neo4j Browser (visual interface)
open http://localhost:7474
```

### Key Demo Queries for Interview
1. **Network Visualization**: Complete supply chain topology
2. **Frenemy Analysis**: Apple-Samsung competition + partnership dynamics
3. **Pathfinding**: Alternative supply routes (Apple → ARM Holdings)
4. **Risk Assessment**: High-value contract dependencies
5. **Market Analysis**: Geographic and concentration risk patterns

This graph analytics framework demonstrates the sophisticated data modeling, query optimization, and business intelligence capabilities essential for the Jefferies Securities Graph Data Engineer role, with direct applications to financial network analysis, risk management, and trading strategy development.