// Neo4j Browser Demo Queries
// Copy and paste these queries into Neo4j Browser (http://localhost:7474)

// 1. Complete Network - Shows all companies and relationships
MATCH (n:Company) OPTIONAL MATCH (n)-[r]-(m:Company) 
RETURN n, r, m;

// 2. Samsung-Apple Frenemy Analysis - Competition + Partnership
MATCH (samsung:Company {name: 'Samsung Electronics Co Ltd'})-[r]-(apple:Company {name: 'Apple Inc'}) 
RETURN samsung, r, apple;

// 3. Market Leaders by Market Cap
MATCH (c:Company) WHERE c.is_final_assembler = true 
RETURN c.name, c.market_cap, c.country 
ORDER BY c.market_cap DESC;

// 4. Supply Chain Path Analysis
MATCH path = shortestPath((apple:Company {name: 'Apple Inc'})-[*]-(arm:Company {name: 'ARM Holdings plc'})) 
RETURN path;

// 4b. Alternative path via TSMC
MATCH path = shortestPath((apple:Company {name: 'Apple Inc'})-[*]-(tsmc:Company {name: 'TSM (Taiwan Semiconductor Manufacturing Co Ltd)'})) 
RETURN path;

// 5. Xiaomi Ecosystem (if loaded)
MATCH (x:Company {name: 'Xiaomi Corporation'})-[r]-(connected) 
RETURN x, r, connected;

// 6. All Relationships by Type
MATCH ()-[r]-() 
RETURN type(r) AS relationship_type, count(r) AS count 
ORDER BY count DESC;